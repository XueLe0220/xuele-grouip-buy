# trade-service 锁单 Redis 库存预占技术演进

## 1. 背景

锁单链路里，参团请求最核心的并发风险是队伍名额被超卖。

业务上需要同时满足：

```text
开新团：团长创建新队伍，天然占用第 1 个名额。
参团：用户加入已有队伍，需要判断队伍仍在进行中、未过期、仍有名额。
失败补偿：如果参团名额已经预占，但后续 DB 落库失败，需要恢复名额。
幂等补偿：如果并发重复请求最终命中已有订单，也要恢复本次多预占的 Redis 名额。
```

本项目锁单 Redis 优化经历了三步：

```text
旧单体 Redisson 双指针抢占
  -> 微服务拆分后的 Redis 预占端口化
  -> Lua 脚本原子化 reserve / recover
```

## 2. 旧单体实现

旧单体里，锁单规则链包含 `TeamStockOccupyRuleFilter`。

参团时会生成两个 Redis key：

```text
teamStockKey：已发出去的占位号计数。
recoveryTeamStockKey：失败或退单恢复出来的名额计数。
```

旧单体核心语义：

```text
1. 开新团不抢占 Redis 库存。
2. 参团时先读 recoveryCount。
3. teamStockKey 自增获取占位号。
4. 判断 occupy <= targetCount + recoveryCount。
5. 如果后续 DB 落库异常，统一 recoveryTeamStock + 1。
6. 退单恢复库存时，也会给 recoveryTeamStock + 1，并用 orderId 做防重。
```

旧单体的优点：

```text
把热点参团名额判断前置到 Redis，减少 DB 热点更新压力。
使用 recovered 计数表达“失败或退单释放出来的名额”，不是简单回退 reserved 指针。
开团和参团语义分离，团长不走 Redis 抢占。
```

旧单体的简化点：

```text
DuplicateKeyException 直接当失败抛出，外层统一恢复 Redis 名额。
恢复库存防重主要在退单链路，锁单失败恢复没有独立 recoveryBizId。
Redis 操作分散在 Java 多次调用里，不是单次原子脚本。
teamStock 自增、判断、恢复之间存在多次 Redis 往返。
```

## 3. 微服务第一版

微服务拆分后，domain 层不能直接依赖 Redis、Redisson 或 Redis key。

因此引入领域端口：

```text
ITeamStockReservationPort
```

由 domain 定义能力：

```java
boolean reserve(String teamId, int targetCount, int currentLockCount, LocalDateTime validEndTime);

void recover(String teamId, String recoveryBizId, LocalDateTime validEndTime);
```

由 infrastructure 实现 Redis 细节：

```text
TeamStockReservationPort
TradeCacheKey
```

Redis key 归属 trade-service infrastructure：

```text
group-buy-trade:team-stock:reserved:{teamId}
group-buy-trade:team-stock:recovered:{teamId}
group-buy-trade:team-stock:recover-biz:{teamId}:{recoveryBizId}
```

微服务规则链顺序调整为：

```text
LockIdempotentRuleFilter
  -> ActivityTrialRuleFilter
  -> UserTakeLimitRuleFilter
  -> TeamAvailableRuleFilter
  -> TeamStockReserveRuleFilter
  -> LockBuildRuleFilter
```

`TeamStockReserveRuleFilter` 放在队伍可用校验之后。

原因：

```text
只有队伍真实存在、状态可参与、未过期、仍有名额时，才有必要进行 Redis 预占。
开新团 teamId 为空，不做 Redis 预占。
参团 reserve 成功后，才把 teamStockReserved 标记到 DynamicContext。
```

## 4. 双指针语义

当前微服务沿用了旧单体的双指针思想，但语义更明确。

```text
reserved：已经发出去的最大占位号。
recovered：已经恢复出来的名额数量。
```

初始化时：

```text
reserved = currentLockCount + recovered
```

不是：

```text
currentLockCount - recovered
```

原因是 `reserved` 不是当前真实占用人数，而是已经发出去过的占位号最大值。

例如：

```text
targetCount = 5
DB currentLockCount = 3
recovered = 2
```

表示历史上已经发出去过：

```text
3 个成功占位 + 2 个失败恢复占位 = 5 个号
```

此时：

```text
reserved = 5
maxReserved = targetCount + recovered = 7
```

后续可以继续发 6、7 两个号，正好对应恢复出来的 2 个名额。

## 5. 幂等补偿调整

微服务版比旧单体更强调幂等友好。

问题出现在并发重复锁单：

```text
1. 两个相同 userId + outTradeNo 请求同时进入。
2. 两个请求入口幂等查询都未命中。
3. 两个请求都完成 Redis 预占。
4. 一个请求 DB 插入个人订单成功。
5. 另一个请求插入个人订单触发唯一索引冲突。
```

旧单体做法：

```text
DuplicateKeyException 直接抛异常。
外层 catch 后恢复 Redis 名额。
用户可能拿到失败。
```

微服务做法：

```text
DB 事务内遇到 DuplicateKeyException 继续抛 INDEX_EXCEPTION，保证事务回滚。
domain service catch 后，如果本次 Redis 已预占，先 recover。
recover 后再按 userId + outTradeNo 查询已有订单。
如果查到已有订单，返回幂等成功。
如果查不到，继续抛异常。
```

这里不能在 repository 的事务里 catch 后直接返回已有订单。

原因：

```text
参团时前面已经执行了 updateAddLockCount。
如果事务里吞掉 DuplicateKeyException 并正常 return，DB lock_count + 1 会被提交。
这会导致 DB 队伍锁单数也多占。
```

因此当前策略是：

```text
事务内抛异常，保证 DB 回滚。
事务外 recover Redis。
recover 后做幂等友好返回。
```

## 6. Lua 脚本优化

微服务第一版可以用 Redisson `RAtomicLong + CAS` 实现 reserve。

但它有几个问题：

```text
一次 reserve 需要多次 Redis 往返。
热点队伍下 CAS 可能自旋重试。
读取 recovered、初始化 reserved、判断库存、递增、设置 TTL 分散在多条命令里。
recover 先 setIfAbsent 再 increment，中间如果 JVM 异常，可能出现防重 key 已写入但 recovered 未加一。
```

因此升级为 Lua 脚本：

```text
team_stock_reserve.lua
team_stock_recover.lua
```

Lua 脚本的价值：

```text
Redis 内部单线程原子执行脚本。
把多次 Redis 命令压缩为一次 eval。
减少网络 RTT。
避免 CAS 自旋。
确保防重 key 写入和恢复计数递增原子完成。
```

### 6.1 reserve 脚本

脚本路径：

```text
group-buy-trade-infrastructure/src/main/resources/redis/script/team_stock_reserve.lua
```

参数约定：

```text
KEYS[1] = reservedKey
KEYS[2] = recoveredKey

ARGV[1] = targetCount
ARGV[2] = currentLockCount
ARGV[3] = ttlMillis
```

脚本语义：

```text
1. 读取 recoveredCount，默认 0。
2. 读取 reservedCount，默认 0。
3. 如果 reservedCount 尚未初始化，则设置为 currentLockCount + recoveredCount。
4. 计算 maxReserved = targetCount + recoveredCount。
5. 如果 reservedCount >= maxReserved，返回 0。
6. 否则 incr reservedKey，设置 TTL，返回 1。
```

返回值：

```text
1：预占成功。
0：预占失败。
```

### 6.2 recover 脚本

脚本路径：

```text
group-buy-trade-infrastructure/src/main/resources/redis/script/team_stock_recover.lua
```

参数约定：

```text
KEYS[1] = recoveredKey
KEYS[2] = recoverBizKey

ARGV[1] = ttlMillis
```

脚本语义：

```text
1. 使用 SET recoverBizKey NX PX ttlMillis 做恢复防重。
2. 如果 recoverBizKey 已存在，返回 0。
3. 如果第一次恢复成功，incr recoveredKey。
4. 设置 recoveredKey TTL。
5. 返回 1。
```

返回值：

```text
1：本次执行了恢复。
0：参数非法、TTL 非法，或 recoveryBizId 已恢复过。
```

## 7. Java 调用方式

Redisson 负责执行 Lua，不负责读取 classpath 下的 `.lua` 文件。

因此由 Spring `ClassPathResource` 加载脚本：

```text
ClassPathResource("redis/script/team_stock_reserve.lua")
ClassPathResource("redis/script/team_stock_recover.lua")
```

执行方式：

```text
redissonClient.getScript().eval(
    RScript.Mode.READ_WRITE,
    scriptText,
    RScript.ReturnType.INTEGER,
    keys,
    args...
)
```

当前先使用 `eval(脚本文本)`。

后续如果需要进一步优化，可以升级为：

```text
scriptLoad + evalSha
```

但第一版没有必要提前复杂化。

## 8. 当前状态

截至本次演进，锁单 Redis 库存预占已经完成：

```text
规则链接入 TeamStockReserveRuleFilter。
开新团跳过 Redis 预占。
参团走 Redis reserve。
reserve / recover 已升级为 Lua 原子脚本。
DB 异常后会 recover。
并发幂等冲突会先 DB 回滚、再 Redis recover、再查询已有订单返回。
trade-service 编译通过。
```

## 9. 后续优化

后续缓存类优化统一处理，当前不展开。

仍可作为后续技术优化点记录：

```text
1. eval 升级为 scriptLoad + evalSha。
2. Redis Cluster 场景下为相关 key 设计 hash tag，确保 Lua 多 key 在同一 slot。
3. 增加 Lua 执行失败监控和告警。
4. 对热点队伍压测 Lua reserve/recover 的耗时和成功率。
5. 结算通知任务阶段参考旧单体，为任务执行增加 Redis 分布式锁防重。
```

## 10. 面试表达

可以这样表达：

```text
锁单参团是高并发热点，直接依赖 DB 条件更新虽然能保证正确性，但热点队伍会给数据库带来较大压力。

旧单体里已经有 Redis 双指针思想：reserved 表示已发出的占位号，recovered 表示失败或退单恢复出来的名额。微服务拆分时，我没有把 Redis 代码直接搬到 domain，而是定义 ITeamStockReservationPort，由 domain 表达“预占和恢复”能力，infrastructure 负责 Redis key、Redisson 和 Lua 脚本。

在微服务版里，参团会先经过队伍可用校验，再执行 Redis reserve。reserve 成功后，如果后续 DB 落库失败或并发幂等冲突，会通过 recoveryBizId 做幂等恢复。对于并发重复锁单，我没有在事务内吞掉 DuplicateKeyException，因为那会导致 DB lock_count 被错误提交，而是让事务回滚，事务外恢复 Redis，再查询已有订单做幂等返回。

最初 reserve 可以用 Redisson AtomicLong + CAS 实现，但热点场景会有多次 Redis 往返和 CAS 重试。因此我进一步把 reserve 和 recover 都改成 Lua 脚本，保证读取、初始化、判满、递增、设置 TTL，以及恢复防重和 recovered 计数递增，都在 Redis 内部一次原子完成。
```

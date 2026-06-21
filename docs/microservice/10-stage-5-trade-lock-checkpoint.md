# 阶段 5：trade-service 锁单链路检查点

## 1. 结论

截至 2026-06-21，`trade-service` 锁单主链路已经形成阶段性闭环。

当前完成的是：

```text
入口契约 -> trigger 入站适配 -> domain 规则链 -> activity-service 试算 -> 锁单聚合构建 -> repository 事务落库 -> 锁单结果返回
```

当前暂未展开的是：

```text
Redis 队伍名额抢占
trade_event_outbox
MQ 投递
成团通知
超时未支付关闭
退款链路
补偿任务
```

这些能力不应该塞进锁单第一版主链路里提前复杂化，后续应跟结算、退款和通知一致性方案一起推进。

## 2. 当前锁单业务链路

锁单入口由 `group-buy-trade-api` 提供 Dubbo 契约：

```text
ITradeOrderService.lockTradeOrder(LockTradeOrderRequestDTO)
```

trigger 层提供两个入口：

```text
TradeOrderProvider：Dubbo Provider
TradeOrderController：本地 HTTP 验证入口
```

trigger 层职责保持很薄：

```text
校验请求参数
LockTradeOrderRequestDTO -> TradeLockCommandEntity
调用 ITradeLockOrderService.lockTradeOrder(...)
TradeOrderEntity -> LockTradeOrderResponseDTO
```

领域服务入口是：

```text
TradeLockOrderService.lockTradeOrder(...)
```

领域服务不直接写库，而是先执行锁单规则链。规则链返回两类结果：

```text
命中幂等：直接返回已有 TradeOrderEntity
未命中幂等：返回 GroupBuyLockAggregate，交给 ITradeRepository.lockOrder(...) 事务落库
```

## 3. 锁单规则链

当前锁单规则链为：

```text
LockIdempotentRuleFilter
  -> ActivityTrialRuleFilter
  -> UserTakeLimitRuleFilter
  -> TeamAvailableRuleFilter
  -> LockBuildRuleFilter
```

各节点职责如下：

```text
LockIdempotentRuleFilter：
按 userId + outTradeNo 查询已有交易单，重复锁单请求直接返回旧订单。

ActivityTrialRuleFilter：
通过 IActivityTrialPort 调用 activity-service 试算，获取活动、商品、价格、目标成团人数、有效期、限购次数、可见性和可参与性等权威快照。

UserTakeLimitRuleFilter：
按 trade-service 自有订单表统计当前用户在当前活动下 status in (CREATE, COMPLETE) 的订单数量，判断是否达到 takeLimitCount。

TeamAvailableRuleFilter：
参团时校验队伍存在、队伍 activityId 与试算 activityId 一致、队伍状态为 PROGRESS、未过期、lock_count < target_count。

LockBuildRuleFilter：
构建 GroupBuyLockAggregate。开团时构建新 GroupBuyTeamEntity；参团时使用已加载队伍；同时构建个人待支付 TradeOrderEntity。
```

## 4. 仓储事务边界

`ITradeRepository.lockOrder(...)` 是锁单落库边界。

当前 repository 实现承担的是交易数据一致性写入，不是简单 CRUD：

```text
事务内复查 userId + outTradeNo 幂等
事务内复查用户参与次数
开新团：插入 group_buy_order，再插入 group_buy_order_list
参团：DB 条件更新 lock_count，再插入 group_buy_order_list
捕获唯一索引冲突并转换为明确业务语义
```

参团增加锁单数时使用 DB 条件更新：

```text
where team_id = ?
  and status = PROGRESS
  and lock_count < target_count
  and valid_end_time > now()
```

这一版选择先用数据库乐观更新保证正确性，暂缓 Redis 队伍名额抢占。原因是当前阶段优先把业务边界、事务边界和失败语义打稳；Redis 抢占属于热点并发优化，后续可以在主链路稳定后补。

## 5. 数据和约束

trade-service 当前拥有两张核心表：

```text
group_buy_order：
拼团队伍维度订单，维护 team_id、activity 快照、价格快照、target_count、lock_count、complete_count、队伍状态、有效期和通知配置。

group_buy_order_list：
用户个人交易订单，维护 user_id、team_id、order_id、activity/goods/price 快照、out_trade_no、biz_id、支付信息和订单状态。
```

锁单阶段关键唯一约束：

```text
group_buy_order.uq_team_id
group_buy_order_list.uq_order_id
group_buy_order_list.uq_out_trade_no
group_buy_order_list.uq_biz_id
```

这些约束配合规则链预校验和 repository 事务内复查，形成三层防线：

```text
规则链预校验
  -> 事务内提交前复查
  -> 数据库唯一约束兜底
```

其中 `out_trade_no` 用于订单级幂等，`biz_id` 用于限制同一用户在同一活动下的参与动作重复创建。

## 6. 与旧单体的核心差异

旧单体锁单链路由 `MarketTradeController` 同时编排营销试算和交易锁单：

```text
MarketTradeController.lockMarketPayOrder
  -> IIndexGroupBuyMarketService.indexMarketTrial(...)
  -> ITradeLockOrderService.lockMarketPayOrder(...)
  -> TradeLockRuleFilterFactory
  -> TradeRepository.lockMarketPayOrder(...)
```

旧单体可以保留的业务语义：

```text
锁单前必须重新试算活动和价格
锁单要区分开团和参团
group_buy_order 表示队伍
group_buy_order_list 表示个人订单
待支付订单和已支付订单都占用用户参与次数
锁单后生成待支付订单
```

旧单体需要修正的问题：

```text
MarketTradeController 同时编排 activity 和 trade，微服务边界不清。
trade 仓储直接读取 activity 表，不适合服务自治。
TradeLockOrderService 构造规则链 command 时没有传 teamId，参团 Redis 名额抢占存在脱节风险。
out_trade_no 和 biz_id 缺少明确唯一约束，幂等和限购并发兜底不足。
锁单查询方法名说查询未支付订单，但 SQL 实际没有 status = CREATE 条件，语义容易混淆。
领域层和 infrastructure 的职责边界不够干净。
```

新 trade-service 的修正：

```text
activity-service 负责活动状态、时间、人群、价格和可参与性。
trade-service 只通过 activity-service API 获取锁单前权威试算快照。
trade-service 自己负责交易侧幂等、限购占用统计、参团队伍合法性、订单冲突、锁单聚合构建和事务落库。
domain 保持纯 Java，不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos、MQ。
服务之间只通过 api 契约通信，不共享 DAO、Mapper、PO、Repository 或 Redis key。
```

## 7. 当前遗留风险

当前锁单主链路可以继续作为结算前置基础，但仍有几个后续优化点：

```text
Redis 队伍名额抢占暂未实现，高热点参团场景后续需要压测后评估。
UserTakeLimitRuleFilter 和 repository 都使用 count 统计参与次数，配合 biz_id 唯一约束可以防超限，但在高并发下可能出现本可参与的第 N 次请求撞唯一索引后失败，后续可设计更明确的限购槽位占用或重试策略。
当前未引入 trade_event_outbox，成团事件和通知补偿需要在结算阶段继续设计。
当前未实现超时未支付关闭，因此 CREATE 订单释放 lock_count 的逆向链路尚未闭环。
当前结算、退款接口仍是占位实现。
```

## 8. 面试表达

可以这样表达当前锁单阶段：

```text
在 trade-service 拆分时，我没有直接复制旧单体的 trade 目录，而是先按服务自治重新划分边界。

锁单前的活动状态、时间、人群可见性、参与资格和优惠价格统一由 activity-service 试算返回，trade-service 不直接访问活动表、优惠表或标签缓存，只保存这次试算产生的交易快照。

trade-service 内部通过责任链表达锁单规则，包括幂等判断、活动试算、用户参与次数限制、参团队伍合法性和锁单聚合构建。落库时由 repository 在本地事务内完成开团或参团，并通过提交前复查和唯一约束兜底并发重复请求。

相比旧单体，新实现解决了 activity 和 trade 边界混杂、幂等唯一约束不足、规则链参数脱节和仓储跨领域访问的问题，同时保留了旧项目中责任链表达交易规则、队伍表和个人订单表分离的业务语义。
```

## 9. 下一步

下一步进入 trade-service 支付结算链路。

结算开始前必须先回看旧单体真实链路：

```text
MarketTradeController.settlementMarketPayOrder
TradeSettlementOrderService
TradeSettlementRuleFilterFactory
OutTradeNoRuleFilter
SCRuleFilter
SettableRuleFilter
TradeSettlementEndRuleFilter
TradeRepository.settlement(...)
group_buy_order_mapper.xml
group_buy_order_list_mapper.xml
```

新结算链路的目标不是简单把订单状态改成已支付，而是完整处理：

```text
支付回调幂等
外部交易单有效性
订单状态校验
支付金额校验
payNo 防串单
队伍状态和有效期边界
个人订单 CREATE -> COMPLETE
队伍 complete_count + 1
成团撞线时 PROGRESS -> COMPLETE
后续 trade_event_outbox 成团事件设计
```

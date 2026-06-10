# 阶段 5：trade-service 拆分设计

## 1. 阶段目标

阶段 5 的目标是把旧单体中的拼团交易能力拆成独立的 `trade-service`。

这个阶段不是把 `domain/trade` 目录复制到新工程，而是要重新明确交易服务的业务边界、数据归属、RPC 契约和一致性方案。

核心边界：

```text
trade-service 负责锁单、支付结算、退款、拼团队伍状态流转和交易补偿任务。
trade-service 拥有 group_buy_order、group_buy_order_list。
notify_task 前期作为 trade-service 本地消息表保留，后续再评估是否拆到 notify-service。
trade-service 不直接访问 activity-service 的活动表、优惠表、SKU 表或标签能力。
trade-service 锁单前通过 activity-service API / Dubbo 做活动试算和参与资格校验。
trade-domain 保持纯 Java，不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos、MQ 或线程池配置。
```

## 2. 业务场景复原

trade-service 面向的是用户发起拼团交易后的全生命周期。

核心角色：

```text
用户：发起开团、参团、支付、取消或退款。
交易系统：保存订单快照、维护队伍状态、处理结算和退款。
activity-service：提供锁单前的活动试算、价格、可见性和可参与性。
支付系统：支付成功后回调 trade-service。
MQ / Job：处理成团通知、退款补偿、超时未支付订单关闭。
```

核心链路：

```text
锁单：
用户提交锁单请求
  -> trade-service 校验请求幂等
  -> 调用 activity-service 试算
  -> 校验可见性、参与资格和价格快照
  -> 开新团或加入已有团
  -> 生成个人待支付订单

支付结算：
支付系统回调
  -> trade-service 校验外部单号、订单状态、支付时间和金额
  -> 个人订单从待支付变为已支付
  -> 队伍 complete_count + 1
  -> 如果达到 target_count，队伍变为成团
  -> 写入成团通知任务

退款：
用户主动退款、业务退款或超时未支付任务触发
  -> trade-service 加载个人订单和队伍状态
  -> 幂等判断
  -> 根据订单状态 + 队伍状态选择退款策略
  -> 更新个人订单、队伍锁单数/完成数/状态
  -> 写入退款通知或库存恢复任务

补偿任务：
定时扫描 notify_task
  -> 执行 MQ 或 HTTP 通知
  -> 成功、失败、重试状态落库
```

## 3. 服务边界

阶段 5 先按完整微服务结构搭建：

```text
group-buy-trade-service
  group-buy-trade-api
  group-buy-trade-domain
  group-buy-trade-infrastructure
  group-buy-trade-trigger
  group-buy-trade-app
```

模块职责：

```text
group-buy-trade-api：
对外暴露锁单、结算、退款 RPC 契约和 DTO。

group-buy-trade-domain：
表达交易领域模型、规则链、状态机、领域服务和端口接口。
不能依赖 Spring、MyBatis、Redis、Dubbo、MQ 或线程池。

group-buy-trade-infrastructure：
实现 trade 仓储、MyBatis DAO、Redis 名额抢占、activity-service Dubbo Consumer 适配。

group-buy-trade-trigger：
实现 Dubbo Provider、本地验证 HTTP Controller、Job、MQ Listener。

group-buy-trade-app：
负责 Spring Boot 启动、配置加载和 Bean 装配。
```

## 4. 数据归属

trade-service 自有表：

```text
group_buy_order：
拼团队伍维度订单，维护 team_id、目标人数、锁单数、完成数、队伍状态、有效期和通知配置。

group_buy_order_list：
用户个人交易单，维护 user_id、order_id、team_id、活动快照、商品快照、价格快照、外部交易单号和订单状态。

notify_task：
前期作为 trade-service 本地消息表，承接成团通知、退款通知、库存恢复等可靠投递任务。
```

不归 trade-service 直接访问的数据：

```text
group_buy_activity、group_buy_discount、sc_sku_activity、sku 归 activity-service。
crowd_tags、crowd_tags_job、crowd_tags_detail、Redis bitmap 归 tag-service。
```

## 5. RPC 关系

阶段 5 首条真实跨服务链路：

```text
trade-service -> activity-service
```

锁单前，trade-service 调用 `IActivityTrialService.trial(ActivityTrialRequestDTO)` 获取：

```text
goodsId、goodsName
originalPrice、deductionPrice、payPrice
targetCount、startTime、endTime
visible、enable
```

当前契约缺口：

```text
旧锁单聚合需要 activityId、validTime 等信息。
现有 activity-service 试算响应没有返回 activityId、validTime。
```

后续有两种选择：

```text
方案 A：trade 锁单请求继续携带 activityId，validTime 后续扩展 activity 试算响应。
方案 B：activity-service 响应补齐 activityId、validTime 等锁单必要快照。
```

推荐后续优先讨论方案 B，因为锁单价格和活动快照最好来自同一次试算结果，减少调用方自行拼装导致的不一致。

## 6. 旧实现诊断

旧单体可以保留的设计：

```text
锁单、结算、退款拆成不同领域服务入口。
锁单和结算使用责任链表达规则校验。
退款使用责任链 + 策略模式表达不同状态组合。
group_buy_order 表示队伍，group_buy_order_list 表示用户个人订单。
notify_task 作为本地消息任务表，适合作为可靠投递雏形。
```

旧实现需要修正的问题：

```text
MarketTradeController 同时编排 activity 和 trade，微服务化后应通过 activity-service API 调用，不能直接依赖 activity-domain。
TradeLockOrderService 构造 TradeLockRuleCommandEntity 时没有传 teamId，参团时 Redis 名额抢占可能失效。
out_trade_no 注释用于幂等，但表结构没有唯一索引。
biz_id 代码里按唯一键冲突处理，但表结构也没有唯一索引。
queryGroupBuyOrderRecordByOutTradeNo 方法名说查未支付订单，SQL 实际没有 status = 0 条件。
幂等查询返回字段缺少 pay_price。
支付结算接口没有金额入参，无法做支付金额校验。
重复支付回调会因 status = 0 更新不到而失败，不是幂等成功。
退款策略枚举和 Spring Bean 名称不一致，已支付未成团、已支付已成团退款可能路由失败。
DataNodeFilter 未判断订单不存在，可能空指针。
notify_task 扫描 SQL 没有查出 uuid，后续按 uuid 更新状态会失败。
MQ Listener 的 topic 语义错位，退单恢复库存逻辑挂在 team success 队列上。
领域层直接依赖 ThreadPoolExecutor 异步执行通知任务，后续应改为本地消息表 + Job 补偿。
```

## 7. 状态机草案

个人订单状态：

```text
CREATE(0)：锁单成功，待支付。
COMPLETE(1)：支付成功。
CLOSE(2)：已关闭或已退单。
```

队伍状态：

```text
PROGRESS(0)：拼单中。
COMPLETE(1)：已成团。
FAIL(2)：失败。
COMPLETE_FAIL(3)：成团后发生部分退单。
```

关键状态流转：

```text
锁单：
无个人订单 -> CREATE
新团：无队伍 -> PROGRESS，lock_count = 1
参团：PROGRESS 且 lock_count < target_count -> lock_count + 1

支付：
CREATE -> COMPLETE
队伍 complete_count + 1
complete_count >= target_count 时 PROGRESS -> COMPLETE

未支付退款：
CREATE -> CLOSE
队伍 lock_count - 1

已支付未成团退款：
COMPLETE -> CLOSE
队伍 lock_count - 1，complete_count - 1

已支付已成团退款：
COMPLETE -> CLOSE
队伍 complete_count - 1
如果最后一人退款，COMPLETE 或 COMPLETE_FAIL -> FAIL
否则 COMPLETE -> COMPLETE_FAIL
```

## 8. 阶段 5 建议小步

第一小步：

```text
新增 trade-service 五模块骨架。
新增阶段 5 设计文档。
不迁移业务逻辑。
```

第二小步：

```text
设计 trade-api 契约：锁单、结算、退款。
先确认 DTO 字段，特别是锁单试算快照、支付金额、幂等键和通知配置。
```

第三小步：

```text
补 trade-service 自有 SQL。
明确唯一索引：out_trade_no、biz_id、notify_task.uuid。
```

第四小步：

```text
先落锁单主链路。
通过 trade-service -> activity-service Dubbo Consumer 完成锁单前试算。
```

第五小步：

```text
再落结算、成团通知、退款和补偿任务。
每条链路都做源码级验收，不只看能否启动。
```

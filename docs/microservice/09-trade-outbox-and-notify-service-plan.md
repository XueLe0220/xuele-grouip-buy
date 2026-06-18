# trade-service 本地事件表与 notify-service 拆分方案

## 1. 结论

锁单链路不引入 `notify_task`。

后续结算、退款、超时关闭等交易状态变化链路，也不建议把旧单体 `notify_task` 原样搬进 trade-service。

推荐拆成两层：

```text
trade-service：
负责交易状态变化，并在同一个本地事务里写入 trade_event_outbox。

notify-service：
负责消费交易事件，生成 notify_task，并执行 HTTP / MQ 通知、重试、告警和状态追踪。
```

一句话原则：

```text
通知投递可以拆出去，但交易事实事件的可靠记录不能拆出 trade-service 的本地事务。
```

## 2. 为什么锁单不做 notify_task

锁单只表示用户占住一个待支付名额，业务状态是：

```text
个人订单：CREATE，待支付
队伍：PROGRESS，拼团中
```

这个阶段还没有形成需要通知下游的稳定交易事实。

锁单成功后，用户可能支付、取消、超时关闭，也可能队伍最终失败。此时如果写通知任务，容易把“过程状态”当成“结果事件”，后续补偿和幂等会变复杂。

所以当前锁单闭环只负责：

- 活动试算
- 用户限购
- 队伍合法性校验
- 待支付订单创建
- 队伍 lock_count 增加
- 幂等和唯一约束

不负责通知任务。

## 3. 旧单体 notify_task 的问题

旧单体中 `notify_task` 同时承担了几类职责：

- 交易状态变化后的可靠任务记录。
- HTTP / MQ 通知参数。
- 重试次数和投递状态。
- 补偿任务扫描条件。

这在单体里可以跑通，但微服务拆分后会造成边界混乱：

```text
trade-service 既负责交易状态，又负责通知投递细节。
notify_task 表既像交易事件表，又像通知任务表。
```

旧实现里还存在一些具体问题：

- 扫描 SQL 没查出 `uuid`，后续按 `uuid` 更新任务状态会失败。
- MQ Listener topic 语义错位，退单恢复库存逻辑挂在成团队列上。
- 领域层曾直接依赖线程池异步通知，破坏 domain 纯净性。
- 通知失败、重试、告警、模板和投递状态没有形成独立边界。

所以后续不建议把旧 `notify_task` 机械迁移到 trade-service。

## 4. 正确边界

trade-service 保留交易事实事件：

```text
trade_event_outbox
```

它表达的是“交易服务内部已经发生的事实”，例如：

- `TEAM_COMPLETED`：队伍成团成功。
- `ORDER_CLOSED`：待支付订单超时关闭。
- `REFUND_CREATED`：退款动作已创建。
- `REFUND_COMPLETED`：退款完成。
- `TEAM_FAILED`：队伍失败。

notify-service 拥有通知投递任务：

```text
notify_task
```

它表达的是“某个事实事件需要如何通知外部系统”，例如：

- HTTP 回调哪个 URL。
- MQ 投递哪个 topic / routing key。
- 当前通知状态。
- 已重试次数。
- 最后失败原因。
- 下一次重试时间。

## 5. 为什么 trade_event_outbox 必须在 trade-service

以支付结算成团为例：

```text
个人订单 CREATE -> COMPLETE
队伍 complete_count + 1
如果达到 target_count，队伍 PROGRESS -> COMPLETE
写入 TEAM_COMPLETED 事件
```

这几步必须在同一个 MySQL 本地事务中提交。

如果 trade-service 更新成团成功后，再通过 RPC 调 notify-service 创建通知任务，一旦 RPC 超时或 notify-service 不可用，就会出现：

```text
交易已经成团，但通知事件丢失。
```

这类问题不能靠简单重试完全解决，因为调用方可能已经不知道远端是否成功。

本地 outbox 的价值就是：

```text
交易状态变化和事件记录同事务提交。
后续由任务或 MQ relay 异步投递。
```

## 6. 后续推荐链路

结算成团链路：

```text
支付回调
  -> trade-service 校验金额、流水号、订单状态
  -> 更新个人订单 COMPLETE
  -> 更新队伍 complete_count
  -> 队伍成团时更新为 COMPLETE
  -> 同事务写 trade_event_outbox(TEAM_COMPLETED)
  -> outbox relay 发布事件
  -> notify-service 消费事件
  -> notify-service 写 notify_task
  -> notify-service 执行 HTTP / MQ 通知
```

退款链路：

```text
退款请求或补偿任务
  -> trade-service 校验退款幂等
  -> 更新个人订单 / 队伍状态
  -> 同事务写 trade_event_outbox(REFUND_CREATED / REFUND_COMPLETED)
  -> outbox relay 发布事件
  -> notify-service 消费事件
  -> notify-service 写退款通知任务
```

超时未支付关闭链路：

```text
trade-service Job 扫描超时 CREATE 订单
  -> 关闭个人订单
  -> 回退队伍 lock_count
  -> 必要时写 ORDER_CLOSED / TEAM_FAILED 事件
  -> notify-service 负责对外通知
```

## 7. 表设计方向

trade-service 后续新增：

```text
trade_event_outbox
```

建议字段：

```text
id
event_id             唯一事件ID
event_type           事件类型
aggregate_type       聚合类型，例如 TEAM / ORDER / REFUND
aggregate_id         聚合ID，例如 team_id 或 order_id
biz_id               业务幂等键
event_status         INIT / SENT / FAILED
payload_json         事件内容快照
retry_count
next_retry_time
create_time
update_time
```

关键约束：

```text
UNIQUE KEY uq_event_id(event_id)
UNIQUE KEY uq_biz_id(biz_id)
KEY idx_status_next_retry_time(event_status, next_retry_time)
```

notify-service 后续拥有：

```text
notify_task
```

建议字段：

```text
id
task_id              通知任务ID
source_event_id      来源事件ID
notify_type          HTTP / MQ
notify_target        URL 或 topic/routing key
notify_status        INIT / SUCCESS / RETRY / FAILED
notify_count
max_retry_count
next_retry_time
last_error
parameter_json
create_time
update_time
```

关键约束：

```text
UNIQUE KEY uq_task_id(task_id)
UNIQUE KEY uq_source_event_id_notify_type(source_event_id, notify_type)
KEY idx_status_next_retry_time(notify_status, next_retry_time)
```

## 8. 当前阶段落点

当前阶段只完成锁单闭环，不实现：

- `trade_event_outbox`
- `notify_task`
- notify-service
- MQ relay
- HTTP 通知
- 通知补偿 Job

这些放到后续阶段：

```text
阶段 5 后半：支付结算 + trade_event_outbox 草案
阶段 7：异步任务与 MQ 拆分
阶段 8：一致性、补偿和幂等完善
阶段 9/10：服务治理、可观测性和通知告警
```

## 9. 面试表达

可以这样表达：

```text
旧单体里 notify_task 既承担交易事件记录，又承担通知投递任务。微服务拆分后，我没有把它原样搬到 trade-service，而是重新拆了边界。

trade-service 只负责交易事实和本地 outbox，保证订单、队伍状态和事件记录在同一个 MySQL 事务里提交。

notify-service 负责具体通知投递，包括 HTTP 回调、MQ 投递、重试次数、失败原因和告警。这样既保证交易事件不丢，也避免 trade-service 被通知细节污染。
```

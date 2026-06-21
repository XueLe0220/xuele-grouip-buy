# 阶段 5：trade-service 支付结算链路检查点

## 1. 结论

截至 2026-06-21，`trade-service` 支付结算主链路已经接入。

当前完成的是：

```text
发起支付准备契约 -> trigger 入站校验 -> domain 支付前校验 -> repository 生成或复用 paymentRequestNo
支付回调契约 -> trigger 入站校验 -> domain 结算规则链 -> repository 本地事务 -> 个人订单和队伍状态推进 -> 结算结果返回
```

当前暂未实现的是：

```text
trade_event_outbox 落表
notify-service
MQ relay
退款
超时未支付关闭
通知补偿任务
```

## 2. 旧单体结算链路

旧单体入口和核心链路：

```text
MarketTradeController.settlementMarketPayOrder
  -> TradeSettlementOrderService.settlement
  -> TradeSettlementRuleFilterFactory
  -> SCRuleFilter
  -> OutTradeNoRuleFilter
  -> SettableRuleFilter
  -> TradeSettlementEndRuleFilter
  -> TradeRepository.settlement(...)
```

旧单体保留的业务语义：

```text
支付成功后个人订单 CREATE -> COMPLETE
队伍 complete_count + 1
complete_count 达到 target_count 后队伍 PROGRESS -> COMPLETE
结算链路使用责任链表达校验和上下文装载
```

旧单体需要修正的问题：

```text
支付结算请求没有 paidAmount 和 payNo，无法校验金额和支付流水。
重复支付回调会因为 status = 0 更新不到而失败，不是幂等成功。
旧结算成功后直接写 notify_task，并在线程池里异步通知，下游通知职责混入 trade-domain。
通知任务和交易事实事件没有拆边界。
```

## 3. 新结算规则链

## 3. 发起支付准备链路

当前在 trade-service 内新增发起支付准备链路：

```text
ITradeOrderService.prepareTradePayOrder(...)
POST /api/trade/pay/prepare
```

它解决的是用户点击支付时的支付前校验，不做真实扣款。

链路职责：

```text
按 userId + outTradeNo 加载个人订单
校验 source、channel 与订单快照一致
校验个人订单仍是 CREATE
加载拼团队伍
校验队伍存在、活动一致、状态为 PROGRESS、未过期
生成或复用 paymentRequestNo
返回 payableAmount、payExpireTime 和支付请求号
```

它不做：

```text
不生成 payNo
不把个人订单改为 COMPLETE
不增加 complete_count
不调用真实支付渠道
```

后续接入 payment-service 时，`paymentRequestNo` 将作为 trade-service 发起支付请求的业务幂等号，由 payment-service 创建支付单并返回支付参数。

## 4. 新结算规则链

新 `trade-domain` 结算规则链：

```text
SettlementOrderLoadRuleFilter
  -> SettlementOrderStatusRuleFilter
  -> PaymentAmountRuleFilter
  -> PaymentNoRuleFilter
  -> TeamSettlementAvailableRuleFilter
  -> SettlementBuildRuleFilter
```

各节点职责：

```text
SettlementOrderLoadRuleFilter：
按 userId + outTradeNo 加载交易单，订单不存在直接失败。

SettlementOrderStatusRuleFilter：
CREATE 订单继续结算；COMPLETE 订单在 payNo、paidAmount 一致时返回幂等成功，已支付但流水或金额不一致时失败；CLOSE 订单拒绝结算。

PaymentAmountRuleFilter：
校验支付回调金额必须等于锁单时保存的 payableAmount。

PaymentNoRuleFilter：
按 payNo 查询是否已绑定其他订单，防止支付流水串单。

TeamSettlementAvailableRuleFilter：
加载队伍，校验队伍存在、活动一致、状态可结算、payTime 不晚于队伍有效期。

SettlementBuildRuleFilter：
构建 GroupBuySettlementAggregate，交给 repository 执行本地事务。
```

## 5. 仓储事务边界

`ITradeRepository.settlementOrder(...)` 是结算落库边界。

事务内完成：

```text
1. 复查 userId + outTradeNo 当前订单。
2. 如果订单已 COMPLETE 且流水、金额一致，返回幂等成功。
3. 如果订单仍是 CREATE，更新个人订单为 COMPLETE，并写入 pay_no、paid_amount、pay_time。
4. 更新队伍 complete_count + 1。
5. 如果 complete_count >= target_count，尝试将队伍 PROGRESS -> COMPLETE。
6. 重新查询订单和队伍，返回结算结果。
```

关键 SQL 防线：

```text
个人订单更新条件：
where user_id = ?
  and out_trade_no = ?
  and status = CREATE

队伍完成数更新条件：
where team_id = ?
  and status = PROGRESS
  and complete_count < target_count

队伍成团更新条件：
where team_id = ?
  and status = PROGRESS
  and complete_count >= target_count
```

## 6. 数据约束

结算阶段建议新增唯一索引：

```sql
UNIQUE KEY uq_payment_request_no(payment_request_no)
UNIQUE KEY uq_pay_no(pay_no)
```

原因：

```text
payment_request_no 是 trade-service 发起支付准备请求号，同一支付准备请求只对应一张交易订单。
pay_no 是支付系统流水号，同一流水不能绑定多个交易订单。
锁单阶段 payment_request_no、pay_no 为 NULL，MySQL 唯一索引允许多个 NULL，不影响待支付订单创建。
发起支付准备写入 payment_request_no，支付成功写入 pay_no，分别由唯一索引兜底并发串单。
```

## 7. 与旧单体的核心差异

新 trade-service 修正为：

```text
结算请求必须携带 paidAmount、payNo、payTime。
trade-service 使用锁单时保存的 payableAmount 做支付金额校验。
重复支付回调表达为幂等成功，而不是 UPDATE_ZERO 失败。
支付流水号做防串单校验，并建议通过 uq_pay_no 唯一索引兜底。
新增发起支付准备链路，把支付前校验从支付回调结算语义中分离出来。
结算领域服务不直接写 notify_task，也不启动线程池通知下游。
成团事件后续应由 trade_event_outbox 在同一 MySQL 本地事务内记录。
```

## 8. 当前遗留风险

```text
当前 prepareTradePayOrder 只返回 paymentRequestNo，不调用真实 payment-service。
trade_event_outbox 尚未落表，成团后的可靠事件投递还未闭环。
如果线上库未添加 uq_pay_no，只靠应用层查询 payNo 仍有并发串单风险。
当前未实现超时未支付关闭，CREATE 订单释放 lock_count 的逆向链路尚未闭环。
当前退款接口仍是占位实现。
```

## 9. 下一步

下一步建议进入：

```text
payment-service 创建支付单链路
trade_event_outbox 表设计与成团事件落库
或 trade-service 退款链路
```

如果继续做结算增强，优先级是：

```text
1. trade_event_outbox(TEAM_COMPLETED) 与结算事务同提交。
2. outbox relay 扫描和发送事件。
3. notify-service 消费交易事件并生成 notify_task。
```

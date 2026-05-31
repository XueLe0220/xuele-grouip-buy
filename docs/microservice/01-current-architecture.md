# 当前系统架构盘点

## 1. 文档目标

本文用于记录当前拼团营销系统的模块职责、核心业务流程、数据表归属和初步架构问题，为后续从模块化单体演进为分布式微服务系统做准备。

当前阶段不急于拆服务，重点是先看清楚系统现状：

- 当前代码是如何分层的
- 锁单、结算、退款流程分别经过哪些组件
- 哪些数据表属于同一业务边界
- 哪些地方在微服务化时风险最高

## 2. 当前模块职责

当前项目是一个 Maven 多模块单体工程，整体风格接近 DDD / 六边形架构。

### 2.1 xuele-group-buy-api

接口契约层。

主要职责：

- 定义对外暴露的服务接口
- 定义请求 DTO、响应 DTO
- 定义统一响应对象

典型内容：

- `IMarketTradeService`
- `IDCCService`
- `LockMarketPayOrderRequestDTO`
- `SettlementMarketPayOrderRequestDTO`
- `RefundMarketPayOrderRequestDTO`
- `Response`

该模块更偏向接口契约，不应该承载具体业务逻辑。

### 2.2 xuele-group-buy-app

应用启动与装配层。

主要职责：

- 提供 Spring Boot 启动入口
- 加载各模块 Bean
- 维护应用配置文件
- 作为当前单体应用的运行入口

典型内容：

- `Application`
- `application.yml`
- `application-dev.yml`
- `logback-spring.xml`

该模块负责把 `trigger`、`domain`、`infrastructure` 等模块组装成一个可运行应用。

### 2.3 xuele-group-buy-domain

领域业务层。

主要职责：

- 定义核心领域模型
- 定义领域服务接口和实现
- 定义仓储接口
- 编排领域规则、责任链、策略模式

典型领域：

- `activity`：活动、优惠、营销试算
- `trade`：锁单、结算、退款、通知任务
- `tag`：人群标签

该模块应尽量表达业务规则，不直接关心 MySQL、Redis、RabbitMQ 等基础设施实现细节。

### 2.4 xuele-group-buy-infrastructure

基础设施适配层。

主要职责：

- 实现领域层定义的 repository / port 接口
- 操作 MySQL、Redis、RabbitMQ
- 维护 MyBatis DAO、PO、Mapper XML
- 提供基础设施配置

典型内容：

- `TradeRepository`
- `ActivityRepository`
- `TagRepository`
- `IGroupBuyOrderDao`
- `IGroupBuyOrderListDao`
- `RedisClientConfig`
- `RabbitMQConfig`

依赖关系上，`infrastructure` 实现 `domain` 中定义的接口。它不应该依赖 `trigger`。

### 2.5 xuele-group-buy-trigger

触发器层。

主要职责：

- 接收 HTTP 请求
- 消费 MQ 消息
- 执行定时任务
- 实现 api 层定义的接口

典型内容：

- `MarketTradeController`
- `DCCController`
- `GroupBuyNotifyJob`
- `TimeoutRefundJob`
- `TeamSuccessTopicListener`
- `RefundSuccessTopicListener`

HTTP、MQ Listener、Job 都可以看作不同类型的触发入口。它们负责把外部事件转成领域服务调用。

### 2.6 xuele-group-buy-types

通用类型层。

主要职责：

- 定义公共枚举
- 定义异常
- 定义常量
- 定义少量通用框架能力

典型内容：

- `ResponseCode`
- `AppException`
- `Constants`
- 责任链相关基础类

该模块会被多个模块依赖，后续拆微服务时需要谨慎控制它的范围，避免变成过大的公共包。

## 3. 当前依赖方向

当前整体依赖可以理解为：

```text
app
 ├── trigger
 ├── infrastructure
 ├── domain
 ├── api
 └── types

trigger -> api / domain / types
domain -> types
infrastructure -> domain / types
api -> types
```

更理想的业务调用方向是：

```text
外部请求 / MQ / 定时任务
        ↓
trigger
        ↓
domain service
        ↓
domain repository interface
        ↓
infrastructure repository implementation
        ↓
MySQL / Redis / MQ / HTTP
```

## 4. 核心业务流程

### 4.1 锁单流程

入口接口：

```text
MarketTradeController.lockMarketPayOrder
```

调用链路：

```text
参数校验
-> 根据 userId + outTradeNo 查询未支付订单，做幂等拦截
-> 如果传入 teamId，查询拼团进度，做容量预校验
-> 调用活动领域做营销试算
-> 调用交易领域执行锁单
-> 执行交易规则责任链
-> 组装锁单聚合
-> TradeRepository.lockMarketPayOrder 落库
```

涉及领域服务：

```text
IIndexGroupBuyMarketService：营销试算、活动可见性、优惠价格计算
ITradeLockOrderService：幂等查询、拼团进度查询、锁单
```

涉及仓储：

```text
ActivityRepository
TradeRepository
```

涉及数据表：

```text
group_buy_activity
group_buy_discount
sc_sku_activity
sku
group_buy_order
group_buy_order_list
```

涉及 Redis / MQ：

```text
Redis：参团时预抢占拼团名额，数据库落库失败后做库存恢复计数
MQ：锁单主流程不直接依赖 MQ
```

关键业务规则：

```text
相同 userId + outTradeNo 不能重复锁单
拼团 lock_count 不能超过 target_count
活动必须存在、生效，并且在活动时间范围内
用户参与次数不能超过活动限购次数
用户必须满足活动人群标签规则
锁单金额必须来自营销试算结果
teamId 为空表示开新团，teamId 不为空表示参团
锁单成功后生成待支付订单
```

关键风险点：

```text
重复请求导致重复订单
高并发参团导致超卖
Redis 抢占成功但数据库落库失败，造成名额虚占
限购判断在并发下被绕过
营销试算和锁单之间存在价格一致性风险
后续拆服务后，trade-service 调 activity-service 试算可能超时或失败
```

### 4.2 结算流程

入口接口：

```text
MarketTradeController.settlementMarketPayOrder
```

调用链路：

```text
参数校验
-> TradeSettlementOrderService.settlement
-> 执行结算规则责任链
   -> source/channel 黑名单校验
   -> outTradeNo 订单有效性校验
   -> 外部支付时间是否在拼团有效期内
   -> 组装结算上下文
-> 构建 GroupBuyTeamSettlementAggregate
-> TradeRepository.settlement 执行本地事务
-> 更新个人订单为已支付
-> 更新团完成数量 complete_count
-> 判断是否达到成团人数
-> 成团则更新团状态，并写入 notify_task
-> 异步执行通知任务
```

涉及领域服务：

```text
ITradeSettlementOrderService：结算流程编排
ITradeTaskService：执行成团后的回调通知任务
```

涉及仓储：

```text
TradeRepository
```

涉及数据表：

```text
group_buy_order_list
group_buy_order
notify_task
```

涉及 Redis / MQ：

```text
结算主事务不依赖 Redis
成团通知通过 notify_task 驱动，后续可能走 HTTP 或 MQ
定时补偿任务使用 Redis 分布式锁防止多实例重复执行
```

关键业务规则：

```text
非法 source/channel 不能结算
只有存在的交易单才能结算
已关闭或已退单订单不能结算
支付时间必须早于拼团有效结束时间
结算成功后个人订单状态变为已支付
团完成人数达到目标人数后，团状态变为成团
成团后必须生成通知任务，保证下游可感知成团结果
```

关键风险点：

```text
支付回调重复通知导致重复结算
complete_count 并发更新导致成团判断错误
订单已超时但支付回调晚到
本地事务成功但通知发送失败
异步通知失败后需要定时任务补偿
后续拆服务后，支付回调、交易状态、通知任务需要保证最终一致性
```

### 4.3 退款流程

入口接口：

```text
MarketTradeController.refundMarketPayOrder
TimeoutRefundJob
```

其中 `MarketTradeController.refundMarketPayOrder` 处理外部主动退款请求，`TimeoutRefundJob` 扫描超时未支付订单并触发自动退款。

调用链路：

```text
参数校验
-> TradeRefundOrderService.refund
-> 执行退款规则责任链
   -> 加载订单和拼团队伍数据
   -> 判断是否重复退单
   -> 根据订单状态 + 团状态选择退款策略
-> 执行具体退款策略
   -> 未支付未成团退款
   -> 已支付未成团退款
   -> 已支付已成团退款
-> 更新个人订单状态、团锁单数、完成数或团状态
-> 写入 notify_task
-> 发送退款相关 MQ / 通知
-> MQ 消费后恢复 Redis 锁单库存
```

涉及领域服务：

```text
ITradeRefundOrderService：退款编排、超时订单查询、恢复锁单库存
ITradeTaskService：执行退款通知任务
```

涉及仓储：

```text
TradeRepository
```

涉及数据表：

```text
group_buy_order_list
group_buy_order
notify_task
```

涉及 Redis / MQ：

```text
Redis：超时退款任务使用分布式锁；退款成功后恢复拼团锁单库存计数
MQ：退款后发送事件，由消费者处理库存恢复或外部退款通知
```

关键业务规则：

```text
已退单订单重复退款时要幂等返回
不同订单状态和团状态对应不同退款策略
未支付未成团：关闭个人订单，释放锁单名额
已支付未成团：关闭个人订单，扣减 lock_count 和 complete_count
已支付已成团：关闭个人订单，必要时调整团状态
超时未支付订单由定时任务自动退款
退款后需要通知下游或恢复库存
```

关键风险点：

```text
重复退款导致库存重复恢复
退款状态组合不完整导致策略选择失败
数据库状态更新成功但 MQ 发送失败
MQ 重复消费导致库存重复恢复
超时退款和支付回调并发到达
已成团退款会影响团状态和后续履约逻辑
```

## 5. 数据表归属

### 5.1 人群标签相关

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

初步归属：

```text
tag-service
```

说明：

这些表用于维护人群标签、标签明细和标签任务。后续拆分时适合作为第一个独立服务，因为它相对独立，和交易强一致性关系较弱。

### 5.2 活动营销相关

```text
group_buy_activity
group_buy_discount
sc_sku_activity
```

初步归属：

```text
activity-service
```

说明：

`group_buy_activity` 存储拼团活动配置，`group_buy_discount` 存储优惠配置，`sc_sku_activity` 表示 source/channel/goods 与 activity 的绑定关系，因此更偏向活动营销域。

### 5.3 交易订单相关

```text
group_buy_order
group_buy_order_list
```

初步归属：

```text
trade-service
```

说明：

`group_buy_order` 表示拼团队伍维度订单，`group_buy_order_list` 表示用户个人锁单/支付订单明细。它们是交易核心表，涉及锁单、结算、退款和成团状态流转。

### 5.4 通知补偿相关

```text
notify_task
```

初步归属：

```text
前期留在 trade-service
后期视情况拆到 notify-service
```

说明：

`notify_task` 当前承担本地消息表和通知补偿职责。它和交易状态变更关系很近，前期不建议急着拆出。等交易主流程稳定后，再考虑独立为通知补偿服务。

### 5.5 商品 SKU 相关

```text
sku
```

初步归属：

```text
当前作为项目内商品模拟表
微服务化后应归属 product-service
```

说明：

真实企业系统中，SKU 通常属于商品服务。当前项目里 `sku` 主要用于支撑营销试算，因此短期可以暂时保留在 activity-service 侧作为模拟表。后续如果引入商品服务，activity-service 应通过接口查询商品信息，或保存活动所需的商品快照。

## 6. 当前最难拆的地方

当前项目最难拆的地方主要集中在交易链路和任务补偿链路。

第一，定时任务不是单纯的调度逻辑，而是会直接参与交易状态变更。例如超时未支付退款任务会扫描订单、触发退款、恢复库存；通知补偿任务会扫描 `notify_task` 并重试下游通知。它们和交易数据、订单状态、MQ 消费存在强耦合。如果过早拆成独立 job-service，容易破坏服务边界，甚至出现跨服务直接访问数据库的问题。

第二，Redis 使用点较多且职责混杂，包括拼团名额抢占、库存恢复、分布式锁、动态配置监听和任务互斥。微服务化后需要重新划分 Redis key 的命名空间、过期策略、幂等语义和封装边界，否则不同服务之间容易互相影响。

第三，交易核心流程同时依赖营销试算、订单状态、拼团库存、MQ 通知和定时补偿。拆分时不仅是移动代码，还要重新设计服务接口、数据归属、事务边界和最终一致性方案。

## 7. 当前发现的问题与后续重构方向

### 7.1 定时任务边界不清晰

当前定时任务位于 `trigger` 层，包括：

```text
GroupBuyNotifyJob
TimeoutRefundJob
```

这些任务表面上是调度逻辑，实际上会调用交易领域服务并修改交易状态。后续拆分时需要明确：

- 哪些任务应留在 trade-service 内部
- 哪些任务可以拆到独立 job-service
- job-service 是否允许直接访问业务数据库
- 任务重复执行时如何保证幂等

现阶段更稳妥的策略是：

```text
交易相关定时任务先留在 trade-service 内部；
通知补偿任务前期也先留在 trade-service；
等 notify-service 成熟后，再考虑把通知补偿独立出去。
```

### 7.2 Redis 职责过多

当前 Redis / Redisson 至少承担以下职责：

```text
基础连接配置
拼团名额抢占
库存恢复计数
分布式锁
DCC 动态配置监听
定时任务互斥执行
```

这些职责的业务含义不同，但目前都直接或间接散落在项目里。后续可以考虑按使用场景抽象：

```text
DistributedLockService：分布式锁
TeamStockService：拼团名额抢占与恢复
DccConfigService：动态配置
JobLockService：任务互斥
```

另外，后续可以评估引入 Zookeeper 承担部分分布式协调能力，例如分布式锁、节点注册、配置监听等。这样可以减轻 Redis 的职责，也能作为学习和面试中的技术扩展点。但是否引入需要结合业务复杂度和维护成本判断，不能只为了堆技术而引入。

### 7.3 退款链路需要重点验证

当前退款链路使用责任链 + 策略模式：

```text
DataNodeFilter
UniqueRefundNodeFilter
RefundOrderNodeFilter
IRefundOrderStrategy
```

已观察到几个后续需要验证的问题：

```text
TradeRefundRuleFilterFactory 中定义了 @Bean，但类本身需要确认是否能被 Spring 正确扫描注册
RefundTypeEnumVO 中的 strategy 名称需要和具体 @Service Bean 名称保持一致
DataNodeFilter 查询订单后需要考虑订单不存在时的异常处理
退款 MQ topic、listener 命名和事件语义需要重新梳理
```

这些问题先记录，不在任务 01 阶段直接修改。

### 7.4 公共模块范围需要控制

`xuele-group-buy-types` 当前作为公共基础层被多个模块依赖。后续拆分为微服务后，公共模块如果过大，容易导致所有服务共享过多内部细节，形成隐性耦合。

后续应控制公共模块只放真正稳定、通用的内容，例如：

```text
统一响应码
基础异常
少量通用常量
通用工具或框架能力
```

业务 DTO、领域模型、数据库 PO 不应随意放入公共模块。

## 8. 任务 01 小结

当前系统虽然是单体应用，但已经具备较好的模块化基础：

- api 层定义接口契约
- trigger 层承接 HTTP / MQ / Job
- domain 层承载核心业务规则
- infrastructure 层适配数据库、Redis、MQ
- types 层提供公共能力

后续微服务化不能简单按技术层拆分，而应按业务边界拆分。初步边界为：

```text
tag-service：人群标签
activity-service：活动、优惠、营销试算
trade-service：锁单、结算、退款、成团
notify-service：通知补偿，后期再拆
product-service：商品 SKU，后期视项目范围引入
```

当前最需要谨慎处理的是交易链路、定时任务、Redis 职责和消息补偿。下一阶段应在此基础上继续梳理微服务边界和企业级改造路线。

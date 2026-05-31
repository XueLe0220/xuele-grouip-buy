# 技术选型与服务边界设计

## 1. 文档目标

本文用于规划拼团营销系统从学习项目向商业化项目演进时的服务边界和技术选型。

本阶段不急于写代码，重点先回答几个问题：

- 系统最终应该拆成哪些服务
- 每个服务负责哪些业务能力
- 哪些数据表归属于哪个服务
- 哪些技术组件解决哪些真实问题
- 哪些现有业务设计需要重构甚至重写

后续改造原则是：先把业务边界设计清楚，再逐步落地技术组件。技术组件必须服务于业务问题，而不是为了堆技术点而引入。

## 2. 商业化改造目标

当前项目是一个拼团营销学习项目，后续目标是将其演进为更接近企业实践的拼团营销平台。

系统定位可以调整为：

```text
面向电商场景的拼团营销平台，支持活动配置、优惠试算、人群筛选、拼团锁单、支付结算、退款补偿、成团通知和服务治理。
```

商业化改造关注点：

```text
业务边界清晰
接口契约稳定
数据归属明确
核心链路幂等
交易状态可追踪
消息通知可补偿
服务调用可降级
系统行为可观测
部署运维可演进
```

## 3. 设计原则

### 3.1 先业务，后技术

微服务拆分必须围绕业务能力，而不是围绕代码分层。

错误拆法：

```text
domain-service
infrastructure-service
trigger-service
```

推荐拆法：

```text
tag-service
activity-service
trade-service
notify-service
product-service
```

### 3.2 一个服务拥有自己的数据

后续微服务化后，应避免一个服务直接访问另一个服务的数据库。

例如：

```text
trade-service 不直接查询 group_buy_activity
trade-service 通过 activity-service 获取活动试算结果

activity-service 不直接查询 crowd_tags_detail
activity-service 通过 tag-service 判断用户是否命中标签
```

### 3.3 交易核心优先保证一致性

交易链路不追求一开始就拆得很细，而是优先保证：

```text
锁单幂等
支付结算幂等
退款幂等
库存恢复正确
通知任务可补偿
状态流转可追踪
```

交易核心服务是最后拆、最谨慎拆的部分。

### 3.4 本地事务 + 可靠消息优先

不优先引入强分布式事务。

推荐策略：

```text
服务内使用本地事务
跨服务状态同步使用 MQ / 本地消息表 / Outbox
失败后通过重试、死信、补偿任务保证最终一致性
```

Seata 可以作为后续研究项，但不作为第一阶段核心依赖。

### 3.5 公共模块保持克制

公共模块只放真正稳定的基础能力：

```text
响应码
基础异常
通用常量
通用工具
少量框架扩展
```

不把业务 DTO、领域模型、数据库 PO 随意放入公共模块，避免服务之间产生隐性耦合。

## 4. 目标服务边界

### 4.1 gateway-service

统一流量入口。

主要职责：

```text
统一路由
统一鉴权
统一限流
跨域处理
请求日志
traceId 注入
灰度路由预留
```

不负责：

```text
业务规则
订单状态修改
数据库访问
```

推荐技术：

```text
Spring Cloud Gateway
Spring Security
JWT
Sentinel Gateway 规则，后续引入
```

### 4.2 auth-service

认证授权服务，前期可以先做轻量版本。

主要职责：

```text
用户登录
JWT 签发
权限校验
网关鉴权支持
```

当前项目没有完整用户体系，因此该服务可以后置。前期可先在 gateway-service 中预留 JWT 鉴权能力。

### 4.3 product-service

商品服务。

主要职责：

```text
维护 SKU 基础信息
提供商品名称、原价、状态等查询能力
为活动试算提供商品基础数据
```

当前项目中的 `sku` 表属于商品模拟表。商业化后应从 activity-service 中独立出去，归属于 product-service。

前期策略：

```text
先保留 sku 表作为商品模拟表
后续拆 product-service 时迁移 sku 表
activity-service 保存活动所需的商品快照，避免交易回放时价格漂移
```

### 4.4 tag-service

人群标签服务。

主要职责：

```text
维护人群标签
维护用户标签明细
处理标签任务
提供用户是否命中标签的判断接口
```

数据归属：

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

适合最先拆分，因为它相对独立，和交易强一致性关系较弱。

对外接口示例：

```text
判断用户是否命中某个标签
查询标签基础信息
提交标签计算任务
查询标签任务状态
```

### 4.5 activity-service

活动与营销试算服务。

主要职责：

```text
维护拼团活动配置
维护优惠规则
维护活动与商品绑定关系
执行营销试算
判断活动可见性与可参与性
调用 tag-service 判断人群规则
调用 product-service 查询商品基础信息，或读取商品快照
```

数据归属：

```text
group_buy_activity
group_buy_discount
sc_sku_activity
```

当前 `sku` 表短期可暂放 activity-service，长期迁移到 product-service。

对外接口示例：

```text
营销试算
查询活动详情
查询商品当前可参与活动
校验用户活动参与资格
```

注意：

```text
activity-service 只负责算规则和价格，不生成交易订单。
```

### 4.6 trade-service

拼团交易服务。

主要职责：

```text
锁单
支付结算
退款
拼团队伍状态流转
订单状态流转
交易幂等
交易本地消息表
超时未支付处理
```

数据归属：

```text
group_buy_order
group_buy_order_list
notify_task，前期保留
```

依赖关系：

```text
trade-service -> activity-service：锁单前进行营销试算
trade-service -> MQ：发送成团、退款、通知事件
trade-service -> Redis：拼团名额预占、热点计数
trade-service -> Zookeeper：低频分布式协调，如定时任务互斥
```

核心原则：

```text
交易服务保存锁单价格快照
交易服务不直接修改活动数据
交易服务不直接查询标签数据
交易状态必须幂等可恢复
```

### 4.7 notify-service

通知补偿服务。

主要职责：

```text
消费成团事件
消费退款事件
执行 HTTP / MQ 下游通知
通知失败重试
死信处理
人工补偿查询
```

前期策略：

```text
notify_task 先留在 trade-service
等交易链路稳定后，再拆 notify-service
```

长期策略：

```text
trade-service 只负责写业务事件
notify-service 负责消费事件、执行通知、维护通知状态
```

### 4.8 job-service

任务调度服务，暂不建议第一阶段独立。

定时任务有两类：

```text
业务强相关任务：例如超时未支付退款
通用调度任务：例如通知补偿扫描、数据清理
```

前期策略：

```text
交易强相关任务留在 trade-service
任务互斥使用 Zookeeper 或 Redis 锁
不允许独立 job-service 直接访问 trade-service 数据库
```

长期策略：

```text
job-service 只负责任务调度
通过 HTTP / RPC 调用业务服务暴露的任务接口
业务状态修改仍由业务服务自己完成
```

## 5. 服务调用关系

目标调用关系：

```text
client
  ↓
gateway-service
  ↓
trade-service
  ↓
activity-service
  ↓
tag-service

activity-service
  ↓
product-service

trade-service
  ↓
MQ
  ↓
notify-service
```

需要避免的调用关系：

```text
activity-service -> trade-service
tag-service -> trade-service
notify-service -> 直接修改交易表
job-service -> 直接查询或修改交易表
跨服务直接访问数据库
```

## 6. 数据边界设计

### 6.1 tag-service

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

### 6.2 activity-service

```text
group_buy_activity
group_buy_discount
sc_sku_activity
activity_product_snapshot，后续新增
```

说明：

`activity_product_snapshot` 可用于保存活动绑定商品时的商品快照，例如商品名称、原价等。这样后续商品价格变化不会影响历史活动和交易。

### 6.3 product-service

```text
sku
product，后续可新增
```

### 6.4 trade-service

```text
group_buy_order
group_buy_order_list
trade_idempotent，后续新增
trade_event_outbox，后续新增
notify_task，前期保留
```

说明：

`trade_idempotent` 可用于记录外部请求幂等键。

`trade_event_outbox` 可用于可靠消息投递，保证本地事务和消息投递最终一致。

### 6.5 notify-service

```text
notify_record，后续新增
notify_retry_record，后续新增
```

前期可继续使用 `notify_task`，等拆出 notify-service 后再迁移。

## 7. 技术选型

### 7.1 Java 与 Spring 基线

推荐基线：

```text
JDK 21
Spring Boot 3.5.x
Spring Cloud 2025.0.x
Spring Cloud Alibaba 2025.0.x
```

选择理由：

```text
当前项目已经使用 Java 21
Spring Cloud 官方兼容表中，2025.0.x 对应 Spring Boot 3.5.x
Spring Cloud Alibaba 2025.0.x 对应 Spring Boot 3.5.x 和 Spring Cloud 2025.0.x
相比直接上 Spring Boot 4，Spring Boot 3.5.x 对学习项目和秋招项目更稳
```

如果短期只做小步改造，也可以先保留当前 Spring Boot 3.2.1，等基础能力补齐后再升级。

### 7.2 注册配置中心

推荐：

```text
Nacos
```

主要用途：

```text
服务注册发现
动态配置
环境隔离
配置灰度
```

选择理由：

```text
与 Spring Cloud Alibaba 生态契合
同时覆盖注册中心和配置中心
适合作为国内 Java 微服务项目的常见选型
```

### 7.3 API 网关

推荐：

```text
Spring Cloud Gateway
```

主要用途：

```text
统一路由
统一鉴权
统一限流
统一跨域
统一日志
统一 traceId
```

选择理由：

```text
Spring Cloud 官方网关组件
适合和 Nacos、Sentinel、OpenFeign 等组件组合
```

### 7.4 服务调用

推荐：

```text
OpenFeign
```

主要用途：

```text
服务间 HTTP 调用
统一声明式客户端
配合负载均衡、超时、重试、降级
```

使用约束：

```text
所有 Feign 调用必须配置超时时间
核心交易链路不做无脑重试
失败要有降级或明确异常
DTO 作为远程契约，不暴露领域对象
```

### 7.5 熔断限流

推荐：

```text
Sentinel
```

主要用途：

```text
接口限流
热点参数限流
服务熔断
系统保护
网关流控
```

选择理由：

```text
Sentinel 与 Spring Cloud Alibaba 生态契合
适合讲清楚高并发场景下的流控、熔断、降级
```

典型应用：

```text
锁单接口限流
营销试算接口限流
用户维度热点限流
activity-service 调用失败时降级
```

### 7.6 消息队列

推荐第一阶段保留：

```text
RabbitMQ
```

主要用途：

```text
成团事件
退款事件
通知重试
异步解耦
削峰填谷
死信队列
```

选择理由：

```text
当前项目已经使用 RabbitMQ
第一阶段保留可以降低改造风险
RabbitMQ 对通知、补偿、异步事件已经足够
```

后续可评估：

```text
RocketMQ
```

但不建议一开始同时替换 MQ 和拆微服务，避免变量过多。

### 7.7 Redis

推荐继续保留。

主要用途：

```text
热点数据缓存
高频计数
拼团名额预占
库存恢复计数
活动/试算结果缓存，后续可做
```

需要收敛的点：

```text
不再让 Redis 同时承担所有分布式协调能力
Redis key 需要统一命名空间
业务代码不直接散落 RedissonClient 操作
按场景封装 TeamStockService、CacheService 等组件
```

### 7.8 Zookeeper

计划引入。

推荐客户端：

```text
Apache Curator
```

主要用途：

```text
分布式锁
Leader 选举
任务调度互斥
服务节点监听，作为学习扩展
```

适合放到 Zookeeper 的场景：

```text
TimeoutRefundJob 多实例互斥执行
GroupBuyNotifyJob 多实例互斥执行
后续 job-service 的 Leader 选举
低频、强一致的分布式协调能力
```

不建议放到 Zookeeper 的场景：

```text
高频库存扣减
高频热点计数
商品/活动缓存
接口限流计数
```

这些场景仍然更适合 Redis。

引入 Zookeeper 的核心理由：

```text
Redis 继续负责高性能缓存和计数
Zookeeper 负责分布式协调
让组件职责更清晰，也便于在面试中讲清楚技术边界
```

注意：

```text
不能为了展示技术而把所有 Redis 锁都替换成 Zookeeper
应该只替换低频任务互斥、Leader 选举等协调类场景
```

### 7.9 数据库与迁移

推荐：

```text
MySQL 8.x
Flyway
MyBatis
```

主要用途：

```text
MySQL：核心业务数据存储
MyBatis：SQL 可控，适合复杂交易更新
Flyway：数据库结构版本化
```

商业化改造时应逐步从 SQL dump 转向版本化迁移脚本。

### 7.10 可观测性

推荐：

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
OpenTelemetry
Loki 或 ELK
```

主要用途：

```text
健康检查
接口指标
JVM 指标
业务指标
链路追踪
日志检索
告警
```

重点业务指标：

```text
锁单成功率
锁单失败原因分布
成团成功率
退款成功率
通知重试次数
MQ 堆积量
接口 P95 / P99 延迟
```

### 7.11 部署运维

推荐演进路线：

```text
Docker Compose 本地开发
Docker 镜像构建
Kubernetes 部署，后期引入
GitHub Actions 或 Jenkins CI/CD，后期引入
```

前期不急着上 Kubernetes，先保证服务拆分、配置、启动和依赖组件可运行。

## 8. 当前不优先引入的技术

### 8.1 Seata

暂不优先引入。

原因：

```text
交易链路更适合先用本地事务 + MQ 最终一致性
Seata 会增加部署、理解和排错成本
过早引入会掩盖业务边界设计问题
```

后续可以作为分布式事务专题研究。

### 8.2 RocketMQ

暂不替换 RabbitMQ。

原因：

```text
当前项目已经接入 RabbitMQ
第一阶段主要目标是边界重构和微服务治理
同时替换 MQ 会增加过多变量
```

后续如果要讲更强的消息事务、顺序消息、延迟消息，可以再评估 RocketMQ。

### 8.3 独立 job-service

暂不第一阶段拆出。

原因：

```text
当前定时任务强依赖交易数据和交易状态
过早拆出容易造成跨服务直接访问数据库
job-service 应只做调度，不应承载交易状态修改
```

## 9. 业务重构方向

用户希望将项目向商业化项目靠齐，因此允许对部分不合理业务设计进行重构甚至重写。

### 9.1 商品与活动边界重构

当前问题：

```text
sku 表混在当前拼团营销系统中
活动试算直接依赖商品基础信息
历史价格快照不够清晰
```

重构方向：

```text
sku 迁移到 product-service
activity-service 维护活动商品绑定关系
trade-service 锁单时保存价格快照
历史交易不受后续商品价格变化影响
```

### 9.2 交易状态机重构

当前问题：

```text
订单状态、团状态、退款状态分散在不同流程中
状态转换规则不够集中
并发下状态流转风险较高
```

重构方向：

```text
明确个人订单状态机
明确拼团队伍状态机
明确退款状态机
所有状态变更必须有幂等条件
核心状态变更使用数据库条件更新保证并发安全
```

### 9.3 通知任务重构

当前问题：

```text
notify_task 同时承担本地消息、通知记录、补偿任务职责
MQ topic 和 listener 命名语义需要重新梳理
```

重构方向：

```text
前期将 notify_task 作为 trade-service 本地消息表
后期拆 notify-service
区分业务事件和通知任务
引入 outbox 模式保证消息可靠投递
```

### 9.4 Redis 使用重构

当前问题：

```text
Redis 同时承担缓存、库存、锁、任务互斥、动态配置等职责
业务代码中 RedissonClient 使用点分散
```

重构方向：

```text
Redis 保留缓存和高频计数
Zookeeper 接管低频分布式协调
封装 TeamStockService、DistributedLockService 等能力
统一 Redis key 命名规范
```

### 9.5 配置治理重构

当前问题：

```text
配置写在 application-dev.yml
数据库、Redis、RabbitMQ 地址和账号密码硬编码
自定义 DCC 与后续 Nacos 配置中心能力重叠
```

重构方向：

```text
引入 Nacos 配置中心
敏感配置通过环境变量或密钥管理
自定义 DCC 后续逐步下线或转为业务开关演示
```

## 10. 推荐演进路线

### 阶段一：商业化单体重构

目标：

```text
不急着拆服务，先把现有单体变得更清晰、更可维护。
```

动作：

```text
梳理状态机
整理 Redis 使用边界
整理配置文件
引入 Flyway
引入 Actuator
补核心测试
修复退款链路潜在问题
```

### 阶段二：基础设施引入

目标：

```text
先搭出微服务运行底座。
```

动作：

```text
引入 Nacos
引入 Gateway
引入 OpenFeign
引入 Sentinel
引入 Zookeeper + Curator
整理 Docker Compose
```

### 阶段三：拆 tag-service

目标：

```text
完成第一个低风险服务拆分。
```

动作：

```text
迁移 crowd_tags 相关表和代码
activity-service 通过接口调用 tag-service
验证注册发现、Feign 调用、超时、降级
```

### 阶段四：拆 activity-service

目标：

```text
将活动试算从交易链路中独立出来。
```

动作：

```text
迁移活动、优惠、商品活动绑定逻辑
trade-service 锁单前远程调用 activity-service
锁单保存试算价格快照
处理 activity-service 超时和降级策略
```

### 阶段五：重构 trade-service

目标：

```text
强化交易核心链路。
```

动作：

```text
重构订单状态机
重构团状态机
引入 outbox
完善幂等表
完善退款补偿
完善通知任务
```

### 阶段六：拆 notify-service

目标：

```text
将通知与补偿能力从交易核心中解耦。
```

动作：

```text
消费交易事件
维护通知记录
支持通知失败重试
支持死信和人工补偿
```

## 11. 面试表达模板

可以这样总结第二阶段设计：

```text
我没有直接把原来的 Maven 模块拆成服务，而是先按业务边界重新设计。最终规划为 gateway、tag、activity、trade、notify、product 等服务。

其中 tag-service 负责人群标签，activity-service 负责活动和营销试算，trade-service 负责锁单、结算、退款和拼团队伍状态流转。交易服务不会直接访问活动和标签表，而是通过接口调用活动服务，活动服务再调用标签服务。

技术选型上，我选择 Nacos 做注册配置中心，Gateway 做统一入口，OpenFeign 做服务调用，Sentinel 做限流熔断，RabbitMQ 做异步事件，Redis 保留高频缓存和库存计数，Zookeeper 负责低频分布式协调，例如定时任务互斥和 Leader 选举。

这样设计的核心目标是让组件职责更清晰：Redis 解决高性能缓存和计数，Zookeeper 解决分布式协调，MQ 解决异步解耦，Nacos 解决服务治理，Sentinel 解决高并发保护。
```

## 12. 参考资料

- Spring Cloud 官方版本说明：https://spring.io/projects/spring-cloud
- Spring Cloud Supported Versions：https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions
- Spring Cloud Alibaba 2025.x 版本说明：https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/
- Nacos Spring Cloud 文档：https://sca.aliyun.com/en/docs/2025.x/user-guide/nacos/quick-start/
- Spring Cloud Gateway 文档：https://docs.spring.io/spring-cloud-gateway/reference/index.html
- Sentinel 官方说明：https://sentinelguard.io/en-us/
- Apache ZooKeeper Releases：https://zookeeper.apache.org/releases
- Apache Curator Recipes：https://curator.apache.org/docs/recipes/

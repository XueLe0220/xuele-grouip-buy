# 阶段 4：activity-service 活动试算核心链路检查点

## 1. 阶段目标

阶段 4 的目标是把旧单体中的活动、优惠和营销试算能力拆到独立的 `activity-service`。

这个阶段不是简单把旧代码搬进一个新 Spring Boot 工程，而是要完成几个边界修正：

```text
activity-service 负责活动配置、商品活动绑定、优惠配置和营销试算。
activity-service 不再直接访问 tag-service 的标签表或 Redis bitmap。
activity-service 通过 tag-service API / Dubbo 判断用户是否命中人群标签。
activity-domain 保持纯 Java，不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos 或线程池配置。
活动试算继续保留规则树和优惠策略模式，不退化成一个大 service 方法。
```

阶段 4 当前完成的是活动试算主链路闭环。

trade-service 锁单前消费 activity-service 的链路放到阶段 5。

## 2. 业务场景

活动试算解决的是用户浏览商品或进入拼团页时，系统根据用户、商品、渠道、活动、人群标签和优惠规则计算本次可见性、可参与性和预估支付价。

一次试算涉及的数据：

```text
用户：userId
商品：goodsId
来源渠道：source / channel
商品活动绑定：sc_sku_activity
活动主配置：group_buy_activity
优惠配置：group_buy_discount
商品价格快照：sku
活动门禁标签：group_buy_activity.tag_id / tag_scope
优惠专享标签：group_buy_discount.tag_id
标签命中判断：tag-service -> Redis bitmap
```

当前验证用例：

```text
userId = xuele
goodsId = 9890001
source = s01
channel = c01
activity_id = 100123
discount_id = 25120208
market_plan = MJ
market_expr = 100,10
tag_id = RQ_KJHKL98UU78H66554GFDV
tag_scope = 1,2
```

`tag_scope = 1,2` 表示活动可见性和参与资格都受人群标签限制。用户未命中标签时，活动不可见、不可参与，优惠也不会生效；用户命中标签后，满减 `100 - 10` 生效，支付价为 `90.00`。

## 3. 完成范围

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

已完成模块：

```text
group-buy-common/group-buy-common-types
group-buy-common/group-buy-common-design
group-buy-activity-service/group-buy-activity-api
group-buy-activity-service/group-buy-activity-domain
group-buy-activity-service/group-buy-activity-infrastructure
group-buy-activity-service/group-buy-activity-trigger
group-buy-activity-service/group-buy-activity-app
```

已完成主要能力：

- `group-buy-common-design` 提供纯 Java 规则树基础能力。
- `group-buy-common-types` 提供通用响应、错误码和字符串基础工具。
- `activity-domain` 完成活动试算规则树节点、领域服务和试算规则引擎。
- `activity-infrastructure` 完成 activity 自有表查询仓储、标签 Dubbo Consumer 端口适配、试算控制端口适配。
- `activity-api` 完成活动试算 RPC 契约和请求 / 响应 DTO。
- `activity-trigger` 完成 Dubbo Provider 和用于本地验证的 HTTP Controller。
- `activity-app` 完成启动类、运行配置、Dubbo / Nacos / MyBatis / datasource 配置和领域对象手动装配。
- activity 相关建表脚本已经写入旧工作区和新微服务工作区。
- Postman 已完成真实链路验证，返回 `deductionPrice=10.00`、`payPrice=90.00`、`visible=true`、`enable=true`。

## 4. 当前源码结构

API 契约：

```text
group-buy-activity-api
  cn.xuele.activity.api.IActivityTrialService
  cn.xuele.activity.api.dto.ActivityTrialRequestDTO
  cn.xuele.activity.api.dto.ActivityTrialResponseDTO
```

Domain：

```text
group-buy-activity-domain
  cn.xuele.activity.domain.service.IActivityService
  cn.xuele.activity.domain.service.ActivityService
  cn.xuele.activity.domain.service.trial.engine.IActivityTrialRuleEngine
  cn.xuele.activity.domain.service.trial.engine.ActivityTrialRuleEngine
  cn.xuele.activity.domain.service.trial.engine.ActivityTrialContext
  cn.xuele.activity.domain.service.trial.engine.AbstractActivityTrialSupport
  cn.xuele.activity.domain.service.trial.node.RootNode
  cn.xuele.activity.domain.service.trial.node.DataLoadNode
  cn.xuele.activity.domain.service.trial.node.TrialControlNode
  cn.xuele.activity.domain.service.trial.node.TagNode
  cn.xuele.activity.domain.service.trial.node.MarketNode
  cn.xuele.activity.domain.service.trial.node.EndNode
  cn.xuele.activity.domain.service.discount.IDiscountCalculateService
  cn.xuele.activity.domain.service.discount.impl.DirectReductionDiscountCalculator
  cn.xuele.activity.domain.service.discount.impl.FullReductionDiscountCalculator
  cn.xuele.activity.domain.service.discount.impl.RateDiscountCalculator
  cn.xuele.activity.domain.service.discount.impl.FixedPriceDiscountCalculator
```

Infrastructure：

```text
group-buy-activity-infrastructure
  cn.xuele.activity.infrastructure.adapter.repository.ActivityRepository
  cn.xuele.activity.infrastructure.adapter.port.TagQueryPort
  cn.xuele.activity.infrastructure.adapter.port.ActivityTrialControlPort
  mybatis/mapper/group_buy_activity_mapper.xml
  mybatis/mapper/group_buy_discount_mapper.xml
  mybatis/mapper/sc_sku_activity_mapper.xml
  mybatis/mapper/sku_mapper.xml
```

Trigger：

```text
group-buy-activity-trigger
  cn.xuele.activity.trigger.rpc.ActivityTrialProvider
  cn.xuele.activity.trigger.http.ActivityTrialController
```

App：

```text
group-buy-activity-app
  cn.xuele.activity.ActivityApplication
  cn.xuele.activity.app.config.DomainServiceConfig
  application-dev.yml
  logback-spring.xml
```

## 5. 核心调用链路

HTTP 验证入口：

```text
POST /api/activity/trial
  -> ActivityTrialController.trial(ActivityTrialRequestDTO)
  -> IActivityService.marketTrial(MarketProductEntity)
  -> IActivityTrialRuleEngine.apply(MarketProductEntity)
  -> RootNode
  -> DataLoadNode
  -> TrialControlNode
  -> TagNode
  -> MarketNode
  -> EndNode
  -> ActivityTrialResponseDTO
```

Dubbo 对外入口：

```text
IActivityTrialService.trial(ActivityTrialRequestDTO)
  -> ActivityTrialProvider
  -> IActivityService.marketTrial(MarketProductEntity)
  -> 同一条 domain 规则树
```

跨服务标签调用：

```text
TagNode
  -> ITagQueryPort.matchUserTag(userId, tagId)
  -> TagQueryPort
  -> @DubboReference ITagQueryService.matchCrowdTag(...)
  -> tag-service
  -> Redis bitmap
```

## 6. 关键设计决策

### 6.1 domain 保持纯净

`activity-domain` 只表达业务规则和领域端口，不直接引用 Spring、MyBatis、Redis、Dubbo、Nacos、HTTP Controller 或线程池。

这样可以保证活动试算规则可以脱离运行环境做单元测试，也避免拆服务后又在 domain 里形成新的技术耦合。

### 6.2 保留规则树而不是大方法

活动试算保留为规则树：

```text
RootNode
  -> DataLoadNode
  -> TrialControlNode
  -> TagNode
  -> MarketNode
  -> EndNode
```

每个节点负责一类业务语义：

```text
RootNode：规则树入口。
DataLoadNode：加载商品、活动绑定、有效活动和优惠配置。
TrialControlNode：处理降级、灰度、切量控制。
TagNode：判断活动可见、可参与和优惠资格。
MarketNode：路由优惠策略，计算优惠金额和支付金额。
EndNode：组装试算结果。
```

这个设计保留了旧单体中的业务编排亮点，同时修正了旧实现中标签、Redis、DCC、Repository 混在规则树节点里的问题。

### 6.3 活动门禁和优惠资格分开

当前明确区分两个标签语义：

```text
group_buy_activity.tag_id：活动门禁标签，控制活动可见 / 可参与。
group_buy_discount.tag_id：优惠专享标签，控制 TAG 类型优惠资格。
```

普通优惠只要活动可见且可参与即可享受。TAG 专享优惠还需要命中优惠自身的人群标签。

如果活动标签和优惠标签相同，`TagNode` 会复用已查询过的命中结果，避免重复调用 tag-service。

### 6.4 标签 RPC 失败不能伪装成 false

`TagQueryPort` 的语义：

```text
Response.success(data) 且 data.matched=false -> 用户真实未命中标签。
RPC 返回 null、Response 失败、data 为空、Dubbo 调用异常 -> 调用失败。
```

调用失败会抛 `AppException`，不会静默返回 `false`。

原因是 `false` 只能表达用户业务上未命中标签，不能表达 tag-service 故障。否则下游故障会被伪装成用户资格不足，排障和补偿都会变困难。

### 6.5 优惠策略 Map 使用业务编码

`MarketNode` 需要 `Map<String, IDiscountCalculateService>`，key 是营销计划编码：

```text
ZJ：直减
MJ：满减
ZK：折扣
N：固定价
```

`DomainServiceConfig` 不依赖 Spring BeanName 作为路由 key，而是按每个策略自己的 `marketPlan()` 显式构建 Map。

原因是营销计划编码是业务路由键，BeanName 只是装配名称。依赖 BeanName 会让重命名 Bean 影响业务路由。

### 6.6 app 层作为组合根

`DomainServiceConfig` 放在 `group-buy-activity-app`。

装配顺序：

```text
EndNode
  -> MarketNode
  -> TagNode
  -> TrialControlNode
  -> DataLoadNode
  -> RootNode
  -> ActivityTrialRuleEngine
  -> ActivityService
```

这样做可以让 domain 继续保持纯 Java，infrastructure 只负责技术适配，app 负责把领域对象和适配器组装成可运行服务。

## 7. API 契约

Dubbo API：

```java
Response<ActivityTrialResponseDTO> trial(ActivityTrialRequestDTO request);
```

请求 DTO：

```text
userId：用户ID
goodsId：商品ID
source：来源
channel：渠道
```

响应 DTO：

```text
goodsId：商品ID
goodsName：商品名称
originalPrice：商品原价
deductionPrice：优惠金额
payPrice：试算支付价
targetCount：成团目标人数
startTime：活动开始时间
endTime：活动结束时间
visible：是否对当前用户可见
enable：是否允许当前用户参与
```

API DTO 是外部契约，domain entity 是内部模型。trigger 负责 DTO 与 domain entity 的转换。

## 8. 运行配置

activity-service 当前运行配置：

```text
server.port = 8092
dubbo.protocol.port = 20892
dubbo.scan.base-packages = cn.xuele.activity.trigger.rpc
datasource schema = group_buy_activity
```

tag-service 当前运行配置：

```text
server.port = 8091
dubbo.protocol.port = 20891
dubbo.scan.base-packages = cn.xuele.tag.trigger.rpc
datasource schema = group_buy_tag
redis key prefix = crowd:tag:bitmap:
```

Nacos 中已经能看到 tag-service 和 activity-service 两个 Dubbo 服务。

## 9. SQL 与数据归属

activity-service 自有 schema：

```text
group_buy_activity
```

activity-service 自有表：

```text
group_buy_activity
group_buy_discount
sc_sku_activity
sku
```

tag-service 自有 schema：

```text
group_buy_tag
```

tag-service 自有表：

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

脚本位置：

```text
旧工作区：
docs/microservice/sql/02-init-group-buy-activity.sql

新微服务工作区：
mysql-microservice/activity/01-init-group-buy-activity.sql
```

注意：当前 Redis bitmap 不会因为 SQL 中存在 `crowd_tags_detail` 自动生成。`tag-service` 查询用户是否命中标签时读取的是 Redis bitmap，不是直接查 `crowd_tags_detail` 表。

## 10. Redis bitmap 前置条件

当前联调用例要求 Redis 中存在：

```text
key = crowd:tag:bitmap:RQ_KJHKL98UU78H66554GFDV
userId = xuele
offset = 19712872
value = 1
```

Docker Redis 配置位置：

```text
docs/microservice/redis/docker-compose.yml
```

当前 Redis 容器信息：

```text
container_name = xuele-redis
宿主机端口 = 16379
容器端口 = 6379
password = xuele_redis_123456
```

可通过以下命令补充测试位：

```bash
docker exec -it xuele-redis redis-cli -a xuele_redis_123456 SETBIT crowd:tag:bitmap:RQ_KJHKL98UU78H66554GFDV 19712872 1
```

验证：

```bash
docker exec -it xuele-redis redis-cli -a xuele_redis_123456 GETBIT crowd:tag:bitmap:RQ_KJHKL98UU78H66554GFDV 19712872
```

返回 `1` 后，`xuele` 才会命中活动门禁标签。

## 11. 联调结果

Postman 请求：

```http
POST http://localhost:8092/api/activity/trial
Content-Type: application/json
```

请求体：

```json
{
  "userId": "xuele",
  "goodsId": "9890001",
  "source": "s01",
  "channel": "c01"
}
```

成功响应：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "goodsId": "9890001",
    "goodsName": "《手写MyBatis：渐进式源码实践》",
    "originalPrice": 100.00,
    "deductionPrice": 10.00,
    "payPrice": 90.00,
    "targetCount": 3,
    "startTime": "2024-12-07T10:19:40",
    "endTime": "2036-12-31T23:59:59",
    "visible": true,
    "enable": true
  },
  "success": true
}
```

这个结果说明以下链路已经跑通：

```text
Postman HTTP
  -> activity-trigger HTTP Controller
  -> activity-domain 试算规则树
  -> activity DB
  -> activity-infrastructure TagQueryPort
  -> tag-service Dubbo Provider
  -> tag-service Redis bitmap
  -> 满减优惠策略
  -> ActivityTrialResponseDTO
```

曾出现的中间结果：

```text
deductionPrice = 0.00
payPrice = 100.00
visible = false
enable = false
```

这个结果不是优惠算法失败，而是 Redis bitmap 未初始化导致用户未命中活动门禁标签。补充 bitmap 后，优惠正常生效。

## 12. 源码级验收

已完成检查：

```text
activity-domain 没有 @Service / @Component / @Repository / @Configuration。
activity-domain 没有 Spring Web / MyBatis / Redis / Dubbo / Nacos 依赖。
activity-domain 没有 ThreadPoolExecutor / CompletableFuture。
TagNode 只依赖 ITagQueryPort，不依赖 tag-service API。
DataLoadNode 只依赖 IActivityRepository。
TrialControlNode 只依赖 IActivityTrialControlPort。
MarketNode 只依赖优惠策略接口和策略 Map。
ActivityTrialProvider 只做入参校验、DTO 转换、异常转换和响应组装。
ActivityTrialController 是本地联调入口，不承载领域逻辑。
```

公司环境说明：

```text
本轮公司环境不再由 AI 执行 Maven 编译和服务启动。
实际运行结果以用户本地启动、Nacos 服务注册和 Postman 联调结果为准。
```

## 13. 当前遗留

阶段 4 核心试算链路已经完成，但仍有后续优化项：

```text
DataLoadNode 查询优化：后续可设计 SKU 查询与活动绑定查询的有限并行。
tag-service bitmap 初始化：后续应补标签任务执行入口或 bitmap 重建机制，而不是手工 SETBIT。
HTTP Controller 定位：当前主要用于本地验证，生产对外 HTTP 入口后续应结合网关、鉴权、日志和统一异常处理重新设计。
trade-service 消费 activity-service：放到阶段 5，由交易锁单链路真实调用 activity Dubbo API。
日志与可观测性：后续在 trigger 入站、RPC 调用失败、DB 查询失败处补充必要日志和 trace。
Dubbo QoS 端口冲突：本机多服务启动时需要规划 QoS 端口或关闭 QoS，避免都抢 22222。
```

## 14. 面试表达

可以这样表达阶段 4：

```text
在拆 activity-service 时，我没有把旧单体的活动试算复制成一个大 service 方法，而是保留规则树作为领域编排。

活动试算被拆成 Root、DataLoad、TrialControl、Tag、Market、End 六个节点，每个节点只依赖领域端口。比如 TagNode 只依赖 ITagQueryPort，不知道 Dubbo、Redis bitmap 或标签表。

标签能力被独立到 tag-service 后，activity-service 不再直接访问 crowd_tags 表或 Redis bitmap，而是通过 ITagQueryService 远程判断用户是否命中标签。这样标签数据、缓存和规则归 tag-service 自治，activity-service 只消费稳定契约。

优惠计算保留策略模式，MarketNode 通过营销计划编码路由到直减、满减、折扣或固定价策略。策略 Map 不是按 Spring BeanName 构建，而是按每个策略的 marketPlan() 构建，避免装配名称影响业务路由。

联调时我们也验证了一个关键故障边界：标签 RPC 调用失败不能被当成 matched=false。false 只表示用户真实未命中标签；调用失败必须抛明确异常，否则会把系统故障伪装成用户资格不足。

最终通过 Postman 验证了 HTTP -> activity-service -> tag-service Dubbo -> Redis bitmap -> 满减优惠计算的完整链路，测试用户 xuele 命中活动标签后，100 元商品满减 10 元，支付价为 90 元。
```


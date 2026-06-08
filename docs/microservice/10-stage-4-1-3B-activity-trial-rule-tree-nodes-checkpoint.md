# 阶段 4-1-3B：activity-service 活动试算规则树节点迁移检查点

## 1. 本阶段目标

本阶段目标是迁移 activity-service 的活动试算规则树节点。

迁移要求不是复制旧单体代码，而是在微服务和六边形架构下保留规则树亮点：

```text
保留活动试算规则树。
保留优惠策略模式。
domain 保持纯 Java。
标签能力通过 ITagQueryPort 接入。
降级、灰度、切量通过 IActivityTrialControlPort 接入。
活动数据读取只通过 IActivityRepository 接入。
```

## 2. 本阶段完成了什么

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

完成范围：

```text
group-buy-common/group-buy-common-types
group-buy-common/group-buy-common-design
group-buy-activity-service/group-buy-activity-domain
```

已完成内容：

- `group-buy-common-design` 新增纯 Java 规则树基础包。
- `group-buy-common-types` 新增 `StringUtils.isBlank()`，避免为了字符串判空引入 Apache Commons。
- `ResponseCode` 新增活动试算相关错误码。
- `GroupBuyActivityDiscountVO` 补充 `isVisible()` / `isEnable()`。
- `ActivityTrialContext` 新增为规则树上下文。
- `AbstractActivityTrialSupport` 新增为活动试算规则树支撑基类。
- `RootNode`、`DataLoadNode`、`TrialControlNode`、`TagNode`、`MarketNode`、`EndNode` 已完成。

当前规则树节点顺序：

```text
RootNode
  -> DataLoadNode
  -> TrialControlNode
  -> TagNode
  -> MarketNode
  -> EndNode
```

## 3. 关键设计决策

### 3.1 使用 common-design，但 activity-domain 再包装 support

`group-buy-common-design` 只提供通用规则树框架：

```text
StrategyHandler
StrategyMapper
AbstractStrategyRouter
```

`group-buy-activity-domain / service / trial / AbstractActivityTrialSupport` 只负责固定泛型：

```text
MarketProductEntity
ActivityTrialContext
TrialBalanceEntity
```

这样做的原因：

```text
common-design 保持通用，不放 activity 业务语义。
activity 节点不用反复写复杂泛型。
后续 trade / settlement 如果使用规则树，也可以复用 common-design，但不会被 activity 类型污染。
```

### 3.2 不迁移旧单体 ErrorNode

旧单体中 `ErrorNode` 用作规则树兜底节点。

本阶段没有迁移它，而是在负责节点直接抛明确业务异常：

```text
DataLoadNode：缺活动绑定、有效活动优惠、SKU 时抛 NO_ACTIVITY_MARKET_CONFIG。
MarketNode：找不到优惠计算策略时抛 NO_DISCOUNT_CALCULATOR。
TrialControlNode：降级、灰度拦截时抛对应错误码。
```

这样做的原因：

```text
异常应该在最了解业务上下文的节点抛出。
避免规则树尾部出现“空结果 + 兜底异常”的绕路逻辑。
减少后续排查问题时的上下文丢失。
```

### 3.3 不迁移旧单体多线程父类

旧单体有 `AbstractMultiThreadStrategyRouter` 和线程池加载逻辑。

本阶段没有迁移它，原因是：

```text
domain 不应该持有 ThreadPoolExecutor。
规则树父类不应该默认背上异步调度职责。
当前阶段目标是业务规则迁移，不是性能优化。
```

后续如果要优化数据加载，只针对 `DataLoadNode` 的数据查询路径单独设计：

```text
querySku 可以和 “querySkuActivity -> queryValidActivityDiscount” 并行。
queryValidActivityDiscount 依赖 skuActivity.activityId，不能和 querySkuActivity 并行。
线程池与异步编排更适合放在 infrastructure 查询编排或专门 adapter 内，不放 domain 父类。
```

### 3.4 TagNode 拆分活动门禁和优惠资格

本阶段明确拆分两个标签语义：

```text
group_buy_activity.tag_id：活动门禁标签，控制活动可见 / 可参与。
group_buy_discount.tag_id：优惠专享标签，控制 TAG 类型优惠资格。
```

`TagNode` 当前处理规则：

```text
活动默认可见且默认可参与：直接放行，不查标签。
活动有限制但未配置 activityTagId：按配置结果保守处理，不误放全量。
活动有限制且配置 activityTagId：调用 ITagQueryPort 判断用户是否命中活动门禁标签。
普通优惠：用户可见且可参与即可享受。
TAG 专享优惠：还需要命中 discount.tagId。
活动标签和优惠标签相同且前面已查过：复用标签命中结果，避免重复调用 tag-service。
```

## 4. 当前源码状态

已完成源码：

```text
group-buy-common-types / common / StringUtils
group-buy-common-types / enums / ResponseCode
group-buy-common-design / framework / tree / StrategyHandler
group-buy-common-design / framework / tree / StrategyMapper
group-buy-common-design / framework / tree / AbstractStrategyRouter
group-buy-activity-domain / model / valobj / GroupBuyActivityDiscountVO
group-buy-activity-domain / service / trial / ActivityTrialContext
group-buy-activity-domain / service / trial / AbstractActivityTrialSupport
group-buy-activity-domain / service / trial / node / RootNode
group-buy-activity-domain / service / trial / node / DataLoadNode
group-buy-activity-domain / service / trial / node / TrialControlNode
group-buy-activity-domain / service / trial / node / TagNode
group-buy-activity-domain / service / trial / node / MarketNode
group-buy-activity-domain / service / trial / node / EndNode
```

当前明确未完成：

```text
ActivityTrialRuleEngine / IActivityTrialRuleEngine 尚未创建或恢复。
ActivityTrialService 尚未创建或恢复。
app 层 DomainServiceConfig 尚未创建。
IActivityTrialControlPort 的 infrastructure adapter 尚未创建。
ITagQueryPort 的 Dubbo infrastructure adapter 尚未创建。
activity-api 尚未定义对外试算 RPC 契约和 DTO。
trigger 尚未提供 HTTP/Dubbo 入站适配器。
DataLoadNode 暂未做异步多线程优化。
```

## 5. 源码级验收

已完成源码级检查：

```text
activity-domain 没有 @Service / @Component。
activity-domain 没有 MyBatis / Redis / Dubbo / Spring Web / Nacos 依赖。
activity-domain 没有 ThreadPoolExecutor / CompletableFuture。
规则树节点中没有 TODO / return null 空实现。
DataLoadNode 只依赖 IActivityRepository。
TagNode 只依赖 ITagQueryPort。
TrialControlNode 只依赖 IActivityTrialControlPort。
MarketNode 通过 Map<String, IDiscountCalculateService> 路由优惠策略。
```

公司环境说明：

```text
本阶段在公司环境只做源码、结构和依赖边界验收。
未执行 Maven 编译、服务启动或 RPC 调用。
编译结果需要用户在公司环境自行执行后反馈。
```

## 6. 下一阶段计划

下一阶段：

```text
阶段 4-1-3C：activity-service 活动试算规则树入口、领域服务与 app 装配。
```

下一阶段优先任务：

```text
1. 新建或恢复 IActivityTrialRuleEngine / ActivityTrialRuleEngine。
2. 新建或恢复 ActivityTrialService。
3. 在 app 层新增 DomainServiceConfig，手动装配规则树节点和优惠策略。
4. 设计并实现 IActivityTrialControlPort 的 infrastructure adapter。
5. 设计 ITagQueryPort 的 Dubbo adapter 依赖方案，再决定是否调整 POM。
```

下一阶段继续遵守：

```text
domain 不加 Spring 注解。
domain 不依赖 MyBatis / Redis / Dubbo / Nacos。
ITagQueryPort 只能通过 tag-service api 契约接入标签能力。
不直接访问 crowd_tags 表或 crowd:tag:bitmap:{tagId}。
不把规则树退化成 ActivityTrialRuleEngine 一个大方法。
不直接迁移旧单体多线程父类。
```

## 7. 面试表达

可以这样表达本阶段：

```text
在 activity-service 拆分时，我没有把旧单体的试算流程复制成一个大 service 方法，而是保留并升级了原来的规则树设计。

我把通用规则树抽到了 common-design，activity-domain 再通过 AbstractActivityTrialSupport 固定业务泛型。这样 common 不被业务污染，activity 节点也不会重复声明泛型。

旧单体里标签、Redis bitmap、DCC、Repository 混在 activity 规则树里。迁移后我把活动数据读取、标签查询、降级灰度都抽成 domain 端口，节点只依赖端口，不感知 MyBatis、Redis、Dubbo 或配置中心。

同时我修正了旧实现里一些边界不清的问题，比如不再迁移 ErrorNode 兜底节点，而是在负责节点抛明确业务异常；也没有把旧的多线程规则树父类照搬到 domain，而是把异步加载留作后续性能优化单独设计。
```

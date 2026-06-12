# 微服务拆分学习协作手册

这份手册用于约束后续“旧单体 -> 新微服务”的学习与重构协作。

它不是越长越好的固定提示词，而是一套工作原则：先讲清业务，再诊断旧实现，最后按企业级最佳实践落地。

## 1. 项目目标

本项目不是把单体项目机械拆成多个 Spring Boot 应用，而是通过一次接近企业真实场景的重构，系统沉淀 Java 后端微服务能力。

重点沉淀：

- DDD 与六边形架构
- 微服务边界、数据归属、缓存归属、RPC 契约
- Dubbo、Nacos、Redis、MySQL、MQ、分布式锁、任务调度
- 并发控制、幂等、一致性、故障边界、配置治理、可观测性
- 项目文档、复盘和面试表达

核心标准：

```text
能跑通只是最低要求。
更重要的是讲清楚业务场景、设计取舍、边界原因、风险和后续扩展。
```

## 2. 项目与路径

旧单体项目只作为业务阅读、设计对照、SQL/数据结构参考，不再作为后续改造对象。

新微服务项目是后续真正落地的项目，服务间调用也只发生在新微服务之间。

公司环境路径：

```text
旧单体项目：D:\Code4J\xuele-group-buy
新微服务项目：D:\Code4J\group-buy-microservice
```

家环境路径：

```text
旧单体项目：E:\Code\Code4Java\xuele-group-buy
新微服务项目：E:\Code\Code4Java\group-buy-microservice
```

路径规则：

- 如果用户已经给出当前工作目录、项目路径或环境信息，AI 直接基于已知路径推进。
- 只有路径不明确、且需要读取/修改/执行命令时，才询问当前是公司环境还是家环境。
- 不要在路径不确定时自行假设旧路径或新路径。

命令规则：

- 公司环境默认只做结构、源码、配置、依赖方向等文件级验收；构建和启动由用户反馈，除非用户明确要求执行。
- 家环境可以执行 Maven 编译、install、服务启动等命令，并把结果纳入验收。

## 3. 协作方式

默认关系是“架构导师 + 结对工程师”。

- 用户说“先讲思路、先别改代码、你打算怎么做”时，只分析和给方案。
- 凡涉及代码、配置、POM、规则文档或阶段文档的修改，AI 必须执行二次确认，不能因为用户说“修、改、实现、继续推进、直接做”而跳过确认。
- 第一次确认：AI 说明本次要改什么、为什么改、涉及哪些文件、关键设计取舍、风险和验收方式，并明确哪些部分建议用户自己主导完成。
- 第二次确认：用户明确通过第一次确认后，AI 复述最终改动清单和不会触碰的范围，再请求最后确认；只有用户第二次明确确认后，AI 才能修改文件。
- 涉及业务迁移、架构边界、Maven 依赖、RPC 契约、并发一致性、规则树、策略模式、事务边界、MQ、分布式锁、幂等或技术亮点时，AI 必须先发出学习警示：这部分建议用户自己主导完成，AI 负责讲解、审查、提示风险和兜底；如果用户仍要求 AI 代做，也必须二次确认。
- 涉及业务迁移、领域模型、聚合、端口、仓储、RPC 契约或状态机设计时，AI 必须先阅读旧单体真实代码和新微服务当前代码，再开始教学或给方案；不能只凭阶段文档、记忆或通用最佳实践推导。
- 教学必须按“旧单体怎么写 -> 旧设计是否合理 -> 新微服务怎么改 -> 为什么这么改”的顺序展开。旧单体不合理时，要明确指出问题来自哪里、会造成什么风险、新设计如何修正。
- 如果某一步只是重复劳动、机械迁移、字段搬运、PO/DAO/Mapper/DTO 成批创建、包名替换或复制粘贴型工作，不涉及复杂业务判断或关键技术取舍，AI 要先提醒用户“这部分主要是重复工作”；即使用户要求批量迁移，也仍然必须完成二次确认后再修改。
- 简单检查可以简洁回答；复杂设计必须讲透业务和原因。
- 每次推进一个小阶段，但小阶段要做到真实闭环，不用 Demo 标准糊弄过去。

二次确认的固定口径：

```text
第一次确认请求：我将修改哪些文件、为什么改、风险是什么、建议你自己完成哪些部分。请回复“第一次确认通过”。
第二次确认请求：我复述最终修改清单和不修改范围。请回复“确认执行修改”。
执行条件：只有收到第二次明确确认，AI 才能调用编辑工具。
```

回答风格：

- 中文回答。
- 先给结论，再讲关键依据。
- 不机械套固定格式。
- 业务和设计问题要讲详细；普通状态同步、简单检查、局部修复要简洁。

## 4. 业务场景优先

每次迁移旧单体能力前，必须先阅读旧单体真实代码，再复原业务场景，而不是直接照着代码搬，也不能脱离旧代码空讲理想设计。

需要讲清楚：

- 这个功能解决什么业务问题。
- 参与角色是谁，例如用户、运营、交易系统、履约系统、定时任务、MQ 消费者。
- 触发入口是什么，例如 HTTP、Dubbo、Job、MQ。
- 核心数据有哪些，例如活动、商品、标签、订单、拼团队伍、支付单、通知任务。
- 状态如何流转，例如待支付、已支付、成团、失败、退款、通知中、通知成功。
- 正常链路怎么走，异常链路怎么走。
- 哪些规则是业务本意，哪些只是旧单体当时为了学习或演示做的简化实现。

输出重点不是“旧代码在哪里”，而是：

```text
旧单体这段业务到底想表达什么。
新微服务里应该由哪个服务负责，为什么。
```

每次讲解复杂业务时，必须同时给出：

- 旧单体涉及的核心类、方法、表或 Mapper。
- 旧单体的真实调用链和状态变更链路。
- 哪些设计可以保留，原因是什么。
- 哪些设计需要重构，原因是什么。
- 新微服务设计和旧单体相比具体改变了什么。

## 5. 旧实现诊断规则

旧单体不是标准答案。它提供业务语义和历史实现参考，但不能无脑照搬结构。

旧实现诊断是必做步骤，不是可选背景阅读。

AI 在给出新设计前，必须先打开并阅读相关旧单体源码。回答中要明确说明参考了哪些旧类或方法；如果暂时没有读取旧代码，必须先说明并补读，不能继续给确定性设计结论。

每次阅读旧代码后，要主动分类：

- 保留：真实业务语义、领域规则、可讲清楚的设计亮点。
- 重构：业务有价值，但边界、依赖、职责或实现方式不适合微服务。
- 废弃：为了演示、临时兜底、空节点、伪实现、无意义包装。
- 补齐：旧单体缺失但真实生产必须考虑的幂等、状态校验、并发、一致性、异常语义、监控告警。

需要重点识别并修正的不合理点：

- domain 混入 MyBatis、Redis、Dubbo、Spring Web、Nacos、线程池等技术细节。
- 活动、标签、交易、结算边界混在一起。
- 一个服务直接访问另一个服务的数据表或缓存。
- 失败时简单返回 false、空对象或静默降级，导致业务语义不清楚。
- 用一个大方法吞掉规则树、责任链、策略模式等原本有表达价值的业务编排。
- 缺少幂等、唯一约束、状态机、并发控制、事务边界和消息一致性设计。
- common 被当成公共垃圾桶，塞入只属于某个业务的模型或工具。
- 配置、缓存 key、数据归属、故障边界没有说清楚。

旧单体改造输出规则：

- 不能只说“最佳实践应该如何”，必须先说“旧单体现在如何”。
- 不能把旧单体的类名、聚合、实体、仓储方法机械映射到新微服务。
- 旧单体中看起来像 DDD 但实际只是流程上下文、数据包或事务参数的对象，要明确指出，不要直接拔高为新服务聚合根。
- 新设计必须解释它修正了旧实现的哪些问题，例如边界污染、职责过重、幂等缺失、事务边界不清、状态机不完整、通知任务不可靠等。

## 6. 最佳实践优先

默认不采用“先最小实现，后面再慢慢补”的方式。

第一次设计就要考虑：

- 后续扩展：规则是否可插拔，策略是否可替换，契约是否稳定。
- 性能风险：是否有 N+1 查询、重复远程调用、热点库存、缓存穿透或锁竞争。
- 并发一致性：是否需要幂等、唯一索引、乐观锁、分布式锁、本地消息表、补偿任务。
- 故障边界：RPC 超时、下游失败、配置异常、MQ 重试、通知失败应该如何表达。
- 服务自治：数据、缓存、配置、任务和部署是否属于本服务。
- 可测试性：领域逻辑是否能脱离 Spring、DB、Redis 做单元验证。
- 面试表达：为什么这样拆，为什么这个边界更清晰，解决了旧实现什么问题。

最佳实践不等于堆复杂度。

原则是：

```text
边界第一次就要正确，扩展点第一次就要留好。
暂时不实现的能力要明确挂起原因和后续落点，不能用错误设计先跑通。
```

## 7. 架构边界

新微服务按业务能力纵向拆分。单个服务内部保持 DDD / 六边形结构。

典型结构：

```text
group-buy-xxx-service/
  group-buy-xxx-api/
  group-buy-xxx-domain/
  group-buy-xxx-infrastructure/
  group-buy-xxx-trigger/
  group-buy-xxx-app/
```

模块职责：

| 模块 | 职责 |
| --- | --- |
| api | 对外 RPC 契约、请求 DTO、响应 DTO |
| domain | 领域模型、领域服务、业务规则、仓储/端口接口 |
| infrastructure | MySQL、Redis、MQ、Dubbo Consumer、外部服务访问、仓储实现 |
| trigger | HTTP、Dubbo Provider、Job、MQ Listener 等入站适配器 |
| app | Spring Boot 启动、配置、Bean 装配 |

推荐依赖方向：

```text
app -> trigger + infrastructure
trigger -> api + domain
infrastructure -> domain + common
domain -> common
api -> common
```

红线：

- domain 不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos、MQ、线程池配置。
- api 不暴露领域内部模型，不依赖 infrastructure。
- infrastructure 可以实现 domain 的端口，但不能把技术实现反向泄漏进 domain。
- common 只放跨服务稳定复用的基础能力，不放具体业务模型。
- 服务之间只通过 api 契约通信，不共享 DAO、Mapper、PO、Repository 或 Redis key。

## 8. 项目亮点保留与升级

需要主动保留和升级的亮点：

- 活动试算规则树
- 优惠计算策略模式
- 交易锁单/结算/退款责任链或规则链
- 六边形端口与适配器
- 服务自治、数据归属、缓存归属
- RPC 契约和故障边界
- 本地消息表、补偿任务、最终一致性
- 配置治理、灰度、降级、可观测性

禁止为了少写代码，把有业务表达价值的规则树、策略链、领域编排退化成一个大方法。

可以重构旧设计，但重构目标必须是：

- 业务语义更清楚。
- 微服务边界更正确。
- 后续扩展更自然。
- 面试表达更有价值。

## 9. 阶段推进方式

每个小阶段按以下思路推进，但不要求每次机械输出完整模板。

1. 源码读取：先打开旧单体相关 Controller、Service、Domain Model、Repository、Mapper、SQL 和新微服务当前代码，确认真实实现。
2. 业务复原：基于旧单体真实代码讲业务场景、流程、数据和状态。
3. 旧实现诊断：指出哪些保留、哪些修正、哪些补齐，并说明对应旧代码位置。
4. 新设计方案：说明服务边界、领域模型、端口、仓储、适配器、依赖变化。
5. 最佳实践检查：补上扩展、性能、并发、一致性、故障语义。
6. 落地实现：需要改代码时，先完成协作方式中的二次确认；用户要求先讲时只分析和给方案。
7. 源码验收：检查真实类、方法、配置、Mapper、依赖方向，不只看编译结果。
8. 命令验收：按环境执行或说明为什么暂不执行。
9. 文档沉淀：阶段完成后写检查点或复盘，再进入下一阶段。

重复工作分流：

- 如果任务只是把已确定的结构、字段、方法、Mapper 或配置按同一模式补齐，先明确告诉用户这部分没有新的复杂业务或技术判断。
- 用户要求“直接迁移”后，AI 仍需完成二次确认；确认通过后再批量修改，重点保证命名、包路径、依赖方向、字段映射、编译结果和边界不出错。
- 不把低价值重复劳动包装成复杂教学；把讲解精力留给业务语义、架构边界、最佳实践和风险判断。
- 即使是机械迁移，也要在完成后做必要源码验收，避免复制粘贴带来的字段错配、包名错误、Mapper id 错误或依赖污染。

源码级验收优先：

- 如果目标是实现 Repository、Service、Controller、Provider、Job、Listener 等具体能力，必须打开源码检查。
- 不能因为 Maven 编译通过就判断业务完成。
- 要识别空实现、伪实现、TODO、错误兜底、边界泄漏和依赖污染。
- 用户要求“校验、验收、检查我写的代码”时，AI 必须完整读取相关文件，而不是只看搜索命中的片段；涉及调用链时必须顺着入口、领域服务、端口、仓储、Mapper、DTO 映射逐层检查。
- 验收必须对照旧单体和新微服务设计目标，说明：旧实现如何，新代码如何，差异是否合理，是否存在遗漏、误改或边界倒退。

Maven 依赖变更前必须说明：

- 具体依赖坐标。
- 父 POM 是否变化。
- 哪些子模块需要新增。
- 哪些模块不应该新增。
- 为什么这样设计，是否破坏六边形边界。

## 10. 总体阶段路线

```text
阶段 0：整体规划
阶段 1：group-buy-common
阶段 2：group-buy-tag-service 骨架
阶段 3：tag-service 真实迁移
阶段 4：activity-service 拆分
阶段 5：trade-service 拆分
阶段 6：settlement-service 拆分
阶段 7：异步任务与 MQ 拆分
阶段 8：分布式锁和一致性
阶段 9：配置中心与服务治理
阶段 10：部署与可观测性
阶段 11：文档与面试表达
```

阶段路线可以调整，但每次调整都要说明原因、收益和风险。

## 11. 当前进度快照

截至 2026-06-10：

1. 阶段 0 已完成：整体规划完成，按业务能力纵向拆分。
2. 阶段 1 已完成：`group-buy-common` 完成，复盘文档：`docs/microservice/04-stage-1-common-review.md`。
3. 阶段 2 已完成：`group-buy-tag-service` 骨架完成，复盘文档：`docs/microservice/05-stage-2-tag-service-skeleton-review.md`。
4. 阶段 3 已完成：tag-service Provider 侧迁移完成，检查点文档：`docs/microservice/06-stage-3-tag-service-checkpoint.md`。
5. 阶段 4 已完成核心链路：`group-buy-activity-service` 已完成活动试算主流程拆分，检查点文档：`docs/microservice/07-stage-4-activity-service-checkpoint.md`。
6. activity-service 已完成 API 契约：`IActivityTrialService.trial(ActivityTrialRequestDTO)`，请求 DTO 包含 `userId`、`goodsId`、`source`、`channel`，响应 DTO 包含价格、优惠、活动时间、可见性和可参与性。
7. activity-domain 已完成活动试算规则树和领域入口：`IActivityService.marketTrial(...)`、`IActivityTrialRuleEngine`、`RootNode`、`DataLoadNode`、`TrialControlNode`、`TagNode`、`MarketNode`、`EndNode`。
8. activity-domain 继续保持纯 Java，不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos 或线程池配置。
9. activity-infrastructure 已完成 activity 自有表仓储、`ITagQueryPort` Dubbo Consumer 适配和 `IActivityTrialControlPort` 基础实现。
10. activity-trigger 已完成 Dubbo Provider `ActivityTrialProvider` 和用于本地验证的 HTTP Controller `ActivityTrialController`。
11. activity-app 已完成 `ActivityApplication`、`DomainServiceConfig`、datasource、MyBatis、Dubbo/Nacos、日志等运行配置。
12. activity-service 已使用独立 schema `group_buy_activity`，自有表包括 `group_buy_activity`、`group_buy_discount`、`sc_sku_activity`、`sku`。
13. activity -> tag-service 真实 Dubbo Consumer 链路已跑通，Nacos 中可见 tag-service 与 activity-service。
14. Postman 已验证 `POST /api/activity/trial`：`userId=xuele`、`goodsId=9890001`、`source=s01`、`channel=c01`，返回 `deductionPrice=10.00`、`payPrice=90.00`、`visible=true`、`enable=true`。
15. 当前联调依赖 tag-service Redis bitmap。测试标签 key 为 `crowd:tag:bitmap:RQ_KJHKL98UU78H66554GFDV`，`xuele` 的 offset 为 `19712872`。
16. 公司环境曾遇到 Maven 3.3.9 + JDK 8、本地仓库缺少当前微服务产物的问题；公司环境默认不由 AI 执行编译和启动，以用户本地运行反馈为准。
17. 阶段 5 已启动：已新增阶段 5 设计文档 `docs/microservice/08-stage-5-trade-service-split-plan.md`。
18. 新微服务项目已创建 `group-buy-trade-service` 五模块骨架：`group-buy-trade-api`、`group-buy-trade-domain`、`group-buy-trade-infrastructure`、`group-buy-trade-trigger`、`group-buy-trade-app`。
19. `group-buy-trade-api` 已完成交易 RPC 契约 `ITradeOrderService`，以及锁单、结算、退款、通知配置 DTO。
20. 锁单请求 DTO 当前保留 `activityId`，但它只表示用户期望参与的候选活动；最终活动、商品、价格、成团人数和有效期必须以 activity-service 重新试算返回为准，不能信任前端状态或价格。
21. 结算请求 DTO 已统一为 `payAmount`、`payNo`、`payTime`，用于后续金额校验、支付流水追踪、对账、退款和防串单。
22. 退款请求 DTO 已包含 `refundRequestNo`，用于表达退款动作自身的幂等键；后续可由调用方传入或由 trade-service 兜底生成。
23. activity-service 试算响应已补 `activityId`、`activityName`、`validTime`，并完成 `TrialBalanceEntity`、`EndNode`、`ActivityTrialProvider`、`ActivityTrialController` 的返回链路透传。
24. trade-domain 已开始按锁单业务小步推进，当前已设计 `TradeLockCommandEntity`、`ActivityTrialEntity`、`TradeOrderEntity`、`GroupBuyTeamEntity`、`LockTypeEnumVO`、`GroupBuyLockAggregate`、`IActivityTrialPort`、锁单阶段的 `ITradeRepository.lockOrder(...)`。
25. `group-buy-common-design` 已迁移旧单体 link 责任链框架：`LinkArmory`、`BusinessLinkedList`、`LinkedList`、`ILink`、`ILogicHandler`，包名为 `cn.xuele.common.design.framework.link`，供后续 trade 锁单规则链使用。
26. trade-service 当前仍未实现 activity Dubbo Consumer、SQL、Provider、Controller 和锁单主链路。

当前阶段 4 遗留优化项：

- DataLoadNode 查询优化：后续可设计 SKU 查询与活动绑定查询的有限并行。
- tag-service bitmap 初始化：后续应补标签任务执行入口或 bitmap 重建机制，而不是手工 SETBIT。
- HTTP Controller 当前主要用于本地验证；生产对外 HTTP 入口后续应结合网关、鉴权、日志和统一异常处理重新设计。
- trade-service 消费 activity-service 放到阶段 5，由交易锁单链路真实调用 activity Dubbo API。
- 日志与可观测性后续在 trigger 入站、RPC 调用失败、DB 查询失败处补齐。
- 本机多服务启动时需要规划 Dubbo QoS 端口或关闭 QoS，避免多个服务抢默认 `22222`。

## 12. 当前接力任务

当前阶段：

```text
阶段 5 已启动。当前只推进 trade-service 锁单业务，不继续扩展结算、退款和通知任务。
```

已完成工作：

1. 阶段 5 设计文档已新增：`docs/microservice/08-stage-5-trade-service-split-plan.md`。
2. `group-buy-trade-service` 五模块骨架已创建。
3. `group-buy-trade-api` 契约和 DTO 已完成。
4. activity-service 试算响应已补锁单所需活动快照字段，并完成返回链路透传。
5. trade-domain 锁单相关模型已初步完成：`TradeLockCommandEntity`、`ActivityTrialEntity`、`TradeOrderEntity`、`GroupBuyTeamEntity`、`LockTypeEnumVO`、`GroupBuyLockAggregate`。
6. trade-domain 锁单相关端口已初步收敛：`IActivityTrialPort`、`ITradeRepository.lockOrder(...)`。
7. common-design 已补 link 责任链框架，后续可用于 trade 锁单规则链。

下一步接力任务：

1. 严格按新版 playbook，先回看旧单体锁单真实链路，再继续设计新 trade-service 锁单规则链。
2. 旧单体锁单规则链是 `ActivityUsabilityRuleFilter -> UserTakeLimitRuleFilter -> TeamStockOccupyRuleFilter -> TradeRuleEndFilter`，但不能原样搬到新微服务。
3. 新 trade-service 锁单规则链应按业务边界重设：activity-service 负责活动状态、时间、人群、价格和可参与性；trade-service 只负责交易侧幂等、参团队伍合法性、订单冲突、锁单聚合构建和落库边界。
4. 推荐下一步先设计锁单规则链骨架：`TradeLockRuleFilterFactory`、`TradeLockRuleContext`、`LockIdempotentRuleFilter`、`ActivityTrialRuleFilter`、`TeamAvailableRuleFilter`、`LockBuildRuleFilter`。
5. 暂缓 `UserTakeLimitRuleFilter` 和 `TeamStockOccupyRuleFilter`。用户限购需要进一步明确 activity-service 与 trade-service 的职责边界；Redis 团队库存抢占属于并发优化，等 DB 乐观更新主链路清楚后再设计。

后续仍需关注：

- trade-service 自有 SQL 与唯一约束：`out_trade_no`、`biz_id`、`notify_task.uuid`。
- 锁单前调用 activity-service 试算，并保存活动、商品、价格快照。
- 支付结算金额校验、支付流水号防串单、重复回调幂等。
- 退款请求幂等、退款状态组合和退款策略路由。
- 本地消息表、MQ 投递、通知补偿任务和超时未支付退单任务。
- 阶段 4 遗留优化仍存在，但当前主线优先推进阶段 5。

## 13. 新对话提示词模板

新开对话时，不要复制整份手册，只复制下面短模板，并补上当前任务。

```text
你是我的 Java 后端微服务架构导师和项目协作伙伴。

项目目标：把旧单体 xuele-group-buy 重构为企业级 group-buy-microservice，用于学习 DDD、六边形架构、微服务拆分、Dubbo、Nacos、Redis、MySQL、MQ、分布式锁、一致性、服务治理和面试表达。

协作要求：
1. 先讲清业务场景，再做架构迁移。
2. 旧单体只作为业务语义和历史实现参考，不能照搬。
3. 主动识别旧实现中为了学习或演示做的简化、不合理边界和伪实现。
4. 默认按最佳实践设计，不把最小实现当最终方案；第一次就要考虑扩展、性能、幂等、一致性、故障边界和面试表达。
5. domain 保持纯净，不能依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos、MQ、线程池配置。
6. 服务之间只通过 api 契约通信，不共享 DAO、Mapper、PO、Repository 或 Redis key。
7. 凡涉及代码、配置、POM、规则文档或阶段文档修改，必须二次确认；即使用户要求修或实现，也不能跳过确认直接推进。
8. 涉及复杂业务、架构边界、RPC 契约、规则树、策略模式、并发一致性、幂等、MQ、分布式锁等技术亮点时，AI 必须先警示用户尽量自己主导完成，AI 负责讲解、审查、提示风险和兜底。

当前进度：
【粘贴 docs/microservice/microservice-learning-playbook.md 的“当前进度快照”和“当前接力任务”中必要部分】

本次任务：
【填写本次要做的小阶段或卡点】
```

## 14. 阶段复盘模板

阶段完成后再复盘，不要每次普通推进都套模板。

```text
1. 本阶段完成了什么
2. 旧单体业务场景是什么
3. 旧实现有哪些问题
4. 我们保留了什么，修正了什么，补齐了什么
5. 为什么这样设计
6. 涉及哪些技术点
7. 企业级体现在哪里
8. 面试官可能怎么问，我应该怎么答
9. 当前还遗留什么风险
10. 下一阶段应该做什么
```

## 15. 面试表达总原则

不要只说“我用了什么技术”，要说清楚：

```text
业务背景 -> 原问题 -> 设计方案 -> 落地实现 -> 验证方式 -> 风险 -> 后续优化
```

推荐表达：

```text
原项目是一个按技术层组织的单体 DDD 学习项目，活动、标签、交易、结算等能力在单体内可以直接互相访问，但这会导致数据归属、缓存归属和故障边界不清晰。

微服务改造时，我按业务能力纵向拆分。每个服务内部保持六边形架构，domain 只表达业务规则，infrastructure 负责 MySQL、Redis、Dubbo、MQ 等技术适配，服务之间只通过 api 契约通信。

例如标签能力拆成 tag-service 后，activity-service 不再直接访问 crowd_tags 表或 Redis bitmap，而是通过 ITagQueryService 判断用户是否命中标签。这样标签数据、缓存和规则归 tag-service 自治，activity-service 只消费稳定契约。
```

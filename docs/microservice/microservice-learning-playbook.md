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

协作模式分为两种：

- 默认教学模式：适合新领域、新架构边界、新业务链路，节奏慢一些，重点是把业务、设计取舍和代码落地讲透。
- 加速推进模式：当用户明确要求“加速、加快、批量推进、直接打闭环”时启用，重点是把已经明确的业务闭环一次性推进到可验收状态。

- 用户说“先讲思路、先别改代码、你打算怎么做”时，只分析和给方案。
- 默认教学模式下，凡涉及代码、配置、POM、规则文档或阶段文档的修改，AI 必须执行二次确认，不能因为用户说“修、改、实现、继续推进、直接做”而跳过确认。
- 第一次确认：AI 说明本次要改什么、为什么改、涉及哪些文件、关键设计取舍、风险和验收方式，并明确哪些部分建议用户自己主导完成。
- 第二次确认：用户明确通过第一次确认后，AI 复述最终改动清单和不会触碰的范围，再请求最后确认；只有用户第二次明确确认后，AI 才能修改文件。
- 涉及业务迁移、架构边界、Maven 依赖、RPC 契约、并发一致性、规则树、策略模式、事务边界、MQ、分布式锁、幂等或技术亮点时，AI 必须先发出学习警示：这部分建议用户自己主导完成，AI 负责讲解、审查、提示风险和兜底；如果用户仍要求 AI 代做，也必须二次确认。
- 涉及业务迁移、领域模型、聚合、端口、仓储、RPC 契约或状态机设计时，AI 必须先阅读旧单体真实代码和新微服务当前代码，再开始教学或给方案；不能只凭阶段文档、记忆或通用最佳实践推导。
- 教学必须按“旧单体怎么写 -> 旧设计是否合理 -> 新微服务怎么改 -> 为什么这么改”的顺序展开。旧单体不合理时，要明确指出问题来自哪里、会造成什么风险、新设计如何修正。
- 如果某一步只是重复劳动、机械迁移、字段搬运、PO/DAO/Mapper/DTO 成批创建、包名替换或复制粘贴型工作，不涉及复杂业务判断或关键技术取舍，AI 要先提醒用户“这部分主要是重复工作”；即使用户要求批量迁移，也仍然必须完成二次确认后再修改。
- 简单检查可以简洁回答；复杂设计必须讲透业务和原因。
- 每次推进一个小阶段，但小阶段要做到真实闭环，不用 Demo 标准糊弄过去。

加速推进模式规则：

- 加速的是协作节奏和重复劳动处理方式，不是降低业务理解、架构边界或最佳实践标准。
- 设计阶段仍必须先讲清业务场景、旧单体真实链路、新微服务职责边界、关键数据、状态变化、异常链路和设计取舍。
- 所谓“最小真实闭环”不是 Demo 版最小实现，而是在最佳实践约束下先打通一个生产语义完整、可验收、可扩展的业务闭环。
- A 类任务：架构边界、RPC 契约、事务边界、幂等、一致性、MQ、分布式锁、规则链核心设计、状态机设计。必须先讲业务和方案，说明风险、验收方式和建议用户主导的部分，用户确认后再实现。
- B 类任务：已确定方案下的 DTO、PO、Mapper、Repository、Provider、Controller、配置类、字段映射、包名调整、同模式批量补齐。AI 说明这是重复性落地工作后，可以在一次批量确认后连续完成，不再逐个文件重复确认。
- C 类任务：明显编译错误、缺失 import、格式修复、简单文档进度同步、局部命名修正。AI 可以直接处理，完成后说明修改点和验收结果。
- 加速模式下，每次推进目标优先定义为一个真实业务闭环，而不是单个类、单个节点或单个 Mapper。
- 加速模式下仍必须做源码级验收，重点检查依赖方向、领域纯净性、RPC 契约、字段映射、唯一约束、事务边界、异常语义和是否存在空实现或伪实现。

trade-service 锁单闭环的加速红线：

- 锁单闭环必须先讲清用户锁单的业务目的、触发入口、活动试算、参团队伍、交易订单、价格快照、待支付状态和失败语义。
- 锁单链路不能为了闭环退化成只插一张订单表；至少要考虑幂等、活动重新试算、参团队伍合法性、订单冲突、价格快照、事务边界、唯一约束、失败返回语义和源码验收。
- activity-service 继续负责活动状态、时间、人群、价格和可参与性；trade-service 负责交易侧幂等、参团队伍合法性、订单冲突、锁单聚合构建和落库边界。
- 暂时不实现的能力必须明确挂起原因和后续落点，不能用错误边界先跑通。

二次确认的固定口径：

```text
第一次确认请求：我将修改哪些文件、为什么改、风险是什么、建议你自己完成哪些部分。请回复“第一次确认通过”。
第二次确认请求：我复述最终修改清单和不修改范围。请回复“确认执行修改”。
执行条件：只有收到第二次明确确认，AI 才能调用编辑工具。
```

加速推进模式下，A 类任务仍优先沿用二次确认；B 类任务可使用一次批量确认；C 类任务可直接处理并在完成后回报。

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

默认教学模式可以按较小粒度推进；加速推进模式下，应把阶段粒度提升到“一个可验收业务闭环”，把重复性文件落地合并处理。

1. 源码读取：先打开旧单体相关 Controller、Service、Domain Model、Repository、Mapper、SQL 和新微服务当前代码，确认真实实现。
2. 业务复原：基于旧单体真实代码讲业务场景、流程、数据和状态。
3. 旧实现诊断：指出哪些保留、哪些修正、哪些补齐，并说明对应旧代码位置。
4. 新设计方案：说明服务边界、领域模型、端口、仓储、适配器、依赖变化。
5. 最佳实践检查：补上扩展、性能、并发、一致性、故障语义。
6. 落地实现：默认教学模式下需要改代码时，先完成协作方式中的二次确认；加速推进模式下按 A/B/C 类任务确认规则推进；用户要求先讲时只分析和给方案。
7. 源码验收：检查真实类、方法、配置、Mapper、依赖方向，不只看编译结果。
8. 命令验收：按环境执行或说明为什么暂不执行。
9. 文档沉淀：从 2026-06-21 起，默认只更新本 playbook 的进度快照、接力任务和风险清单，不再额外新建项目拆解、阶段计划、检查点或复盘文档；除非用户明确要求，或某个设计需要长期留痕到独立文档。

文档推进规则：

- 本 playbook 是后续微服务改造的唯一接力文档，任务推进、阶段状态、下一步选择和遗留风险都优先写回这里。
- 历史阶段文档继续作为参考资料保留，但后续普通推进不再新增同类拆解文档。
- 如果新增独立文档，必须先说明为什么 playbook 不够承载、该文档后续如何维护，避免文档散落后过期。

重复工作分流：

- 如果任务只是把已确定的结构、字段、方法、Mapper 或配置按同一模式补齐，先明确告诉用户这部分没有新的复杂业务或技术判断。
- 默认教学模式下，用户要求“直接迁移”后，AI 仍需完成二次确认；确认通过后再批量修改，重点保证命名、包路径、依赖方向、字段映射、编译结果和边界不出错。
- 加速推进模式下，重复工作可以在一次批量确认后连续修改，但修改完成后必须做源码级验收，不能只汇报“已生成文件”。
- 不把低价值重复劳动包装成复杂教学；把讲解精力留给业务语义、架构边界、最佳实践和风险判断。
- 即使是机械迁移，也要在完成后做必要源码验收，避免复制粘贴带来的字段错配、包名错误、Mapper id 错误或依赖污染。

业务闭环优先：

- 加速推进模式下，优先围绕“入口 -> 领域编排 -> 端口 -> 适配器 -> 数据落库 -> 返回语义 -> 验收方式”组织任务。
- 每个闭环都必须能说明业务上完成了什么，而不是只说明新增了哪些类。
- 不能为了尽快闭环牺牲最佳实践。边界、扩展点、幂等、事务、唯一约束、失败语义和后续补偿落点必须在第一版设计中说清楚。
- 对暂缓实现的能力，要明确是“正确设计下的延后落点”，不是“先用错误方式绕过”。

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
阶段 6：payment-service / 支付适配演进
阶段 7：trade_event_outbox、MQ relay 与 notify-service
阶段 8：分布式锁和一致性
阶段 9：tag-service 用户画像、Kafka 事件流与 Agent 辅助标签治理
阶段 10：project-agent-service 项目治理助手
阶段 11：配置中心、部署、可观测性与面试表达
```

阶段路线可以调整，但每次调整都要说明原因、收益和风险。

## 11. 当前进度快照

截至 2026-06-21：

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
14. Postman 已验证 `POST /api/activity/trial`：`userId=xuele`、`goodsId=9890001`、`source=s01`、`channel=c01`，返回 `deductionPrice=10.00`、`payableAmount=90.00`、`visible=true`、`enable=true`。
15. 当前联调依赖 tag-service Redis bitmap。测试标签 key 为 `crowd:tag:bitmap:RQ_KJHKL98UU78H66554GFDV`，`xuele` 的 offset 为 `19712872`。
16. 公司环境曾遇到 Maven 3.3.9 + JDK 8、本地仓库缺少当前微服务产物的问题；公司环境默认不由 AI 执行编译和启动，以用户本地运行反馈为准。
17. 阶段 5 已启动：已新增阶段 5 设计文档 `docs/microservice/08-stage-5-trade-service-split-plan.md`。
18. 新微服务项目已创建 `group-buy-trade-service` 五模块骨架：`group-buy-trade-api`、`group-buy-trade-domain`、`group-buy-trade-infrastructure`、`group-buy-trade-trigger`、`group-buy-trade-app`。
19. `group-buy-trade-api` 已完成交易 RPC 契约 `ITradeOrderService`，以及锁单、结算、退款、通知配置 DTO。
20. 锁单请求 DTO 当前保留 `activityId`，但它只表示用户期望参与的候选活动；最终活动、商品、价格、成团人数和有效期必须以 activity-service 重新试算返回为准，不能信任前端状态或价格。
21. 结算请求 DTO 已统一为 `paidAmount`、`payNo`、`payTime`，用于后续金额校验、支付流水追踪、对账、退款和防串单。
22. 退款请求 DTO 已包含 `refundRequestNo`，用于表达退款动作自身的幂等键；后续可由调用方传入或由 trade-service 兜底生成。
23. activity-service 试算响应已补 `activityId`、`activityName`、`validTime`，并完成 `TrialBalanceEntity`、`EndNode`、`ActivityTrialProvider`、`ActivityTrialController` 的返回链路透传。
24. trade-domain 已开始按锁单业务小步推进，当前已设计 `TradeLockCommandEntity`、`ActivityTrialEntity`、`TradeOrderEntity`、`GroupBuyTeamEntity`、`LockTypeEnumVO`、`GroupBuyLockAggregate`、`IActivityTrialPort`、锁单阶段的 `ITradeRepository.lockOrder(...)`。
25. `group-buy-common-design` 已迁移旧单体 link 责任链框架：`LinkArmory`、`BusinessLinkedList`、`LinkedList`、`ILink`、`ILogicHandler`，包名为 `cn.xuele.common.design.framework.link`，供后续 trade 锁单规则链使用。
26. trade-service 锁单主链路已完成阶段性闭环，检查点文档：`docs/microservice/10-stage-5-trade-lock-checkpoint.md`。
27. `group-buy-trade-trigger` 已完成 Dubbo Provider `TradeOrderProvider` 和本地验证 HTTP Controller `TradeOrderController`，锁单入口统一走 `ITradeLockOrderService.lockTradeOrder(...)`。
28. trigger 层已完成锁单请求参数校验、`LockTradeOrderRequestDTO -> TradeLockCommandEntity` 转换，以及 `TradeOrderEntity -> LockTradeOrderResponseDTO` 返回映射。
29. trade-domain 已完成锁单领域服务 `TradeLockOrderService`，通过责任链完成锁单业务规则编排，命中幂等时直接返回已有订单，未命中时构建锁单聚合并交由仓储落库。
30. trade-domain 已完成锁单规则链：`LockIdempotentRuleFilter -> ActivityTrialRuleFilter -> UserTakeLimitRuleFilter -> TeamAvailableRuleFilter -> LockBuildRuleFilter`。
31. `LockIdempotentRuleFilter` 按 `userId + outTradeNo` 查询已有交易单，作为重复锁单请求的业务幂等入口。
32. `ActivityTrialRuleFilter` 通过 `IActivityTrialPort` 调用 activity-service 试算，activity-service 继续负责活动状态、活动时间、人群可见性、参与资格和价格计算；trade-service 保存试算返回的活动、商品和价格快照。
33. `UserTakeLimitRuleFilter` 已纳入锁单链路，按 trade-service 自有订单表统计当前用户在活动下的待支付和已支付订单数量，判断是否达到 `takeLimitCount`。
34. `TeamAvailableRuleFilter` 已完成参团队伍校验：队伍存在、活动一致、状态为拼团中、未过期、`lock_count < target_count`。
35. `LockBuildRuleFilter` 已完成 `GroupBuyLockAggregate` 构建，区分开新团和参团，生成队伍快照与个人待支付订单快照。
36. trade-infrastructure 已完成 `ActivityTrialPort`，通过 Dubbo Consumer 调用 `IActivityTrialService.trial(...)`，并将 activity-service DTO 转换为 trade-domain 的 `ActivityTrialEntity`。
37. trade-infrastructure 已完成 `TradeRepository.lockOrder(...)` 事务落库：开新团时插入 `group_buy_order` 和 `group_buy_order_list`；参团时先通过 DB 条件更新增加 `lock_count`，再插入个人订单。
38. trade-service 锁单落库阶段已补提交前防线：事务内复查幂等和限购，并依赖 `team_id`、`order_id`、`out_trade_no`、`biz_id` 唯一约束兜底并发重复写入。
39. trade-service 支付结算主链路已接入，检查点文档：`docs/microservice/11-stage-5-trade-settlement-checkpoint.md`。
40. 结算入口已支持 Dubbo Provider `ITradeOrderService.settlementTradeOrder(...)` 和本地 HTTP `POST /api/trade/settlement`。
41. 发起支付准备入口已支持 Dubbo Provider `ITradeOrderService.prepareTradePayOrder(...)` 和本地 HTTP `POST /api/trade/pay/prepare`。
42. 发起支付准备链路已形成简单闭环：trigger 校验 `userId/source/channel/outTradeNo`，domain 校验订单存在、来源渠道一致、订单为 `CREATE`、队伍为 `PROGRESS`、队伍未过期，repository 通过 `for update` 锁定个人订单后生成或复用 `paymentRequestNo`。
43. 当前支付准备链路只返回 `paymentRequestNo`、`payableAmount`、`payExpireTime`、订单状态和队伍状态，不生成 `payNo`，不推进订单 `COMPLETE`，不增加 `complete_count`，也不调用真实 payment-service 或微信/支付宝等第三方接口。
44. 结算请求 DTO 使用 `paidAmount`、`payNo`、`payTime`，已补齐旧单体缺失的支付金额校验、支付流水防串单和重复支付回调幂等语义。
45. trade-domain 已完成结算规则链：`SettlementOrderLoadRuleFilter -> SettlementOrderStatusRuleFilter -> PaymentAmountRuleFilter -> PaymentNoRuleFilter -> TeamSettlementAvailableRuleFilter -> SettlementBuildRuleFilter`。
46. trade-infrastructure 已完成 `ITradeRepository.settlementOrder(...)` 事务落库：个人订单 `CREATE -> COMPLETE` 并写支付信息，队伍 `complete_count + 1`，撞线时 `PROGRESS -> COMPLETE`。
47. 发起支付阶段建议在 `group_buy_order_list.payment_request_no` 上增加唯一索引 `uq_payment_request_no`；结算阶段建议在 `group_buy_order_list.pay_no` 上增加唯一索引 `uq_pay_no`。
48. trade-service 退单主链路已接入，支持 Dubbo Provider `ITradeOrderService.refundTradeOrder(...)` 和本地 HTTP `POST /api/trade/refund`。
49. 退单链路已保留旧单体“规则链 + 状态组合路由”的表达价值，并修正旧单体空指针、策略 Bean 名称耦合、通知职责混入 domain 等问题。
50. trade-domain 已完成退单规则链：`RefundOrderLoadRuleFilter -> RefundIdempotentRuleFilter -> RefundTypeRuleFilter -> RefundBuildRuleFilter`。
51. 当前退单状态组合覆盖：未支付退单 `CREATE + PROGRESS -> CLOSE` 且队伍 `lock_count - 1`；已支付未成团退款 `COMPLETE + PROGRESS -> CLOSE` 且队伍 `lock_count - 1、complete_count - 1`；已支付已成团退款 `COMPLETE + COMPLETE/PARTIAL_REFUND -> CLOSE` 且队伍 `complete_count - 1`，队伍转 `PARTIAL_REFUND` 或 `FAIL`。
52. trade-infrastructure 已完成 `ITradeRepository.refundOrder(...)` 本地事务落库，事务内通过 `for update` 锁定个人订单和队伍记录，重复 `CLOSE` 订单幂等返回。
53. 当前退单链路不调用真实 payment-service 或第三方退款接口，不写 MQ，不生成 notify_task；退款流水、退款请求号持久化、trade_event_outbox、超时关单和通知补偿任务仍未实现。

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
阶段 5 已完成 trade-service 锁单、支付准备、支付结算和退单主链路。不单独拆 settlement-service。
```

已完成工作：

1. 历史阶段文档已保留为参考：`08-stage-5-trade-service-split-plan.md`、`10-stage-5-trade-lock-checkpoint.md`、`11-stage-5-trade-settlement-checkpoint.md`；后续普通推进不再新增同类拆解文档，只更新本 playbook。
2. `group-buy-trade-service` 五模块骨架已创建。
3. `group-buy-trade-api` 契约和 DTO 已完成。
4. activity-service 试算响应已补锁单所需活动快照字段，并完成返回链路透传。
5. trade-service 锁单模型、端口、规则链、Provider、Controller、Activity Dubbo Consumer、Repository、DAO、Mapper 和 SQL 已形成主链路闭环。
6. trade-service 发起支付准备链路已完成：校验可支付状态，通过 `for update` 锁定个人订单，生成或复用 `paymentRequestNo`，返回应付金额和支付有效期；当前只是支付前准备，不调用真实 payment-service 或第三方支付接口。
7. trade-service 结算规则链、领域服务、Provider、本地 HTTP、Repository、DAO 和 Mapper 已形成主链路闭环。
8. trade-service 退单规则链、领域服务、Provider、本地 HTTP、Repository、DAO 和 Mapper 已形成主链路闭环。
9. common-design 已补 link 责任链框架，当前已用于 trade-service 锁单、结算和退单规则链。

下一步接力任务：

1. 下一步优先推进 trade-service 后半段一致性设计：`trade_event_outbox` 表设计，并在结算成团、退单、订单关闭等事务内落交易事实事件。
2. 退款增强的下一步是设计 `trade_refund_order` 或等价退款流水表，持久化 `refundRequestNo`、退款原因、退款金额、退款状态和支付流水关联，补齐退款动作级幂等。
3. 支付链路仍有两条可选路线：A. 设计独立 payment-service，让 trade-service 通过端口用 `paymentRequestNo` 创建支付单；B. 先做轻量内部支付准备能力，只维护支付请求记录和状态，不接第三方支付接口。
4. 如果选择 payment-service，必须先讲清 payment-service 是否真的需要独立拆分、它拥有哪些数据、是否对接第三方、和 trade-service 的幂等/回调/退款边界如何划分。
5. 如果选择轻量内部支付准备，优先补齐 `payment_request_no` 唯一约束、支付准备状态、支付请求幂等和过期语义，保持不接第三方接口。
6. 结算已完成主链路，但上线前需要确保数据库执行 `uq_payment_request_no(payment_request_no)` 和 `uq_pay_no(pay_no)` 唯一索引。
7. 本轮仍不扩展超时关单、Redis 队伍名额抢占、MQ 投递和通知补偿。
8. 后续任务推进完成后只更新本 playbook 的进度快照和接力任务，除非用户明确要求，不再输出新的项目拆解文档。

秋招增强任务池：

1. 目标定位：在 2026 年秋招前，把项目从“微服务拆分练习”打磨成“微服务交易系统 + 事件驱动用户画像 + 任务型 Agent 工程化”的组合项目；重点面向 ToB、ERP、制造业、互联网中厂和中小厂后端岗位表达。
2. 第一优先级是 tag-service 用户画像增强：补齐“用户标签从哪里来、怎么打、怎么解释”的业务闭环，不只停留在 Redis bitmap 查询。
3. 用户画像推荐链路：`业务事件 -> Kafka -> 用户行为特征聚合 -> 标签规则引擎 -> tag-service 标签表/bitmap -> activity-service 人群判断`。
4. 可消费的业务事件包括：`activity_trial`、`trade_locked`、`pay_prepared`、`order_paid`、`team_completed`、`refund_requested`、`notify_failed`。
5. 可沉淀的用户特征包括：近 7/30 天试算次数、锁单次数、支付转化率、优惠敏感度、参团偏好、成团成功率、退款倾向、活跃时间段、支付准备后未支付次数。
6. 可生成的标签包括：新用户、高优惠敏感用户、高转化拼团用户、频繁试算未支付用户、退款风险用户、高价值用户、沉默用户。
7. Agent 在用户画像链路中的边界：Agent 不直接参与核心交易状态流转，也不实时决定用户是否可参与活动；Agent 主要用于标签规则建议、用户画像解释、运营策略复盘和标签效果分析。
8. 用户画像 Agent 的推荐能力：运营输入“找高优惠敏感用户”时，Agent 基于已有特征和规则模板给出标签规则建议，并说明命中原因、风险和可回放验证方式。
9. 用户画像 Agent 的解释能力：查询某个用户为什么命中某标签时，Agent 读取用户特征、标签规则和历史事件，输出可审计解释，而不是只返回大模型主观判断。
10. 第二优先级是 project-agent-service：作为独立工程治理助手，读取 playbook、代码结构、接口契约、SQL、状态机和 Git diff，辅助理解业务、生成 Mermaid 状态机、检查改造进度。
11. project-agent-service 后续可扩展只读运维工具：检查云端服务器中 MySQL、Redis、Nacos、Kafka、Dubbo 服务状态，读取健康指标、端口、日志摘要和 Kafka 消费积压。
12. project-agent-service 的安全边界：默认只读；涉及重启服务、清理数据、修改配置、执行 SQL 等危险操作必须人工确认；工具调用要有日志和审计。
13. 7 月下旬前优先做可演示小闭环：Kafka 行为事件模型、用户特征聚合、标签规则落库/bitmap 更新、Agent 标签解释、project-agent 基于 playbook 的业务问答和状态机输出。
14. 8 月底到 9 月初前再补工程深度：trade_event_outbox 可靠投递、Kafka relay、标签规则版本化和回放、project-agent 中间件健康检查、消费积压诊断、面试表达整理。
15. 面试表达重点：不是“我接了大模型接口”，而是“核心交易链路保持稳定可审计，Agent 放在运营辅助和工程治理场景，通过工具调用读取真实数据和文档，提升画像解释、规则治理和项目维护效率”。

后续仍需关注：

- trade-service 自有 SQL 与唯一约束：锁单阶段关注 `team_id`、`order_id`、`out_trade_no`、`biz_id`；后续结算、退款阶段再设计 `trade_event_outbox.event_id`、`trade_event_outbox.biz_id`。
- 锁单前调用 activity-service 试算，并保存活动、商品、价格快照。
- 锁单 Redis 参团队伍双指针已从 Redisson `RAtomicLong + CAS` 升级为 Lua 原子脚本，`reserve/recover` 的读取、初始化、判满、占位、恢复防重和过期设置已合并到 Redis 内部一次执行；详细演进见 `docs/microservice/12-trade-lock-redis-evolution.md`。
- 支付结算金额校验、支付流水号防串单、重复回调幂等。
- 退款请求号持久化、退款流水表、退款金额和支付流水关联。
- 本地消息表、MQ 投递、通知补偿任务和超时未支付关单任务。
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
9. 后续任务推进默认只更新 playbook 的进度快照、接力任务和风险清单，不再额外输出项目拆解、阶段计划、检查点或复盘文档，除非我明确要求。

当前进度：
【粘贴 docs/microservice/microservice-learning-playbook.md 的“当前进度快照”和“当前接力任务”中必要部分】

本次任务：
【填写本次要做的小阶段或卡点】
```

## 14. Playbook 内复盘模板

仅当用户明确要求复盘，或某个阶段需要在 playbook 内沉淀面试表达时使用。默认不要新建独立复盘文档，也不要每次普通推进都套模板。

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

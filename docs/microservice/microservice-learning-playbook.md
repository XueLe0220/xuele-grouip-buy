# 微服务拆分学习协作手册

## 1. 项目目标

本项目的目标不是简单把单体项目拆成多个 Spring Boot 应用，而是通过一次接近企业真实项目的重构过程，系统学习并沉淀 Java 后端微服务架构能力。

旧单体项目路径：

```text
E:\Code\Code4Java\xuele-group-buy
```

新微服务项目路径：

```text
E:\Code\Code4Java\group-buy-microservice
```

当前存在公司环境与家环境两套路径。

公司环境路径：

```text
旧单体项目路径：D:\Code4J\xuele-group-buy
新微服务项目路径：D:\Code4J\group-buy-microservice
```

家环境路径：

```text
旧单体项目路径：E:\Code\Code4Java\xuele-group-buy
新微服务项目路径：E:\Code\Code4Java\group-buy-microservice
```

路径确认规则：

```text
每次新对话开始时，如果本次任务需要读取、检查、修改项目文件，或者需要执行命令，但用户没有明确说明当前使用公司路径还是家路径，AI 必须先询问：当前使用公司路径还是家路径？

在路径不确定时，禁止自行假设项目目录，禁止直接使用旧路径执行检查或修改。
```

本项目需要重点沉淀：

- DDD 与六边形架构
- 微服务服务边界拆分
- Dubbo RPC 调用
- Nacos 注册中心与配置中心
- Redis 缓存与库存控制
- MySQL 数据归属与表边界
- MQ 异步解耦与最终一致性
- 分布式锁与任务抢占
- 定时任务拆分
- 服务治理、配置治理、部署运维
- 项目文档与面试表达

核心标准：

```text
能跑通功能只是最低要求。
更重要的是能讲清楚为什么这样拆、为什么这样设计、解决了什么问题、还有什么风险。
```

底层推进原则：

```text
最佳实践优先，不以最小实现作为最终设计。
保留旧项目中有价值的架构亮点，但必须在微服务边界和六边形架构下重新落地。
迁移不是复制粘贴，必须主动识别旧业务中不合理的依赖、边界和职责混杂，并在当前阶段尽早修正。
每推进一个能力，都要同时思考是否能沉淀新的项目亮点、面试表达点和企业级设计点。
```

强制提醒：

```text
如果某个实现方案只是为了快速跑通，却会损失项目亮点、架构表达或后续扩展性，AI 必须明确指出，并优先给出更符合企业实践的方案。

规则树、责任链、策略模式、六边形端口、服务自治、数据归属、缓存归属、RPC 契约、配置治理、故障边界等，都是本项目需要主动保留和挖掘的亮点。

不能为了少写代码，把原本有业务表达价值的规则树/策略链退化成一个大方法。
```

## 2. 总体项目规划

新项目采用“按业务能力纵向拆分”的方式组织，每个业务服务都是独立工程，服务内部继续保持 DDD / 六边形结构。

当前规划：

```text
group-buy-microservice/
  group-buy-common/
    group-buy-common-types/
    后续可能扩展：
      group-buy-common-starter-web/
      group-buy-common-starter-dubbo/
      group-buy-common-starter-redis/

  group-buy-tag-service/
    group-buy-tag-api/
    group-buy-tag-domain/
    group-buy-tag-infrastructure/
    group-buy-tag-trigger/
    group-buy-tag-app/

  后续继续拆分：
    group-buy-activity-service/
    group-buy-trade-service/
    group-buy-settlement-service/
    group-buy-job-service/
```

### 2.1 common 工程定位

`group-buy-common` 不是微服务，不启动，不注册 Nacos，也没有端口。

它是公共基础 jar 工程，用于沉淀跨服务稳定复用的基础能力。

当前只做：

```text
group-buy-common-types
```

允许放入：

- `Response<T>`
- `ResponseCode`
- `AppException`
- `Constants`
- 后续可能加入 `PageRequest`、`PageResponse`

禁止放入：

- Tag 业务模型
- Activity 业务模型
- Trade 业务模型
- MyBatis / Redis / Dubbo / Nacos 配置
- 只服务某个业务的工具类

原则：

```text
common-types 是公共基础类型库，不是公共垃圾桶。
```

### 2.2 单个业务服务内部结构

以 `group-buy-tag-service` 为例：

```text
group-buy-tag-service/
  group-buy-tag-api/
  group-buy-tag-domain/
  group-buy-tag-infrastructure/
  group-buy-tag-trigger/
  group-buy-tag-app/
```

职责说明：

| 模块 | 职责 |
| --- | --- |
| `api` | 对外契约，放 Dubbo 接口、请求 DTO、响应 DTO |
| `domain` | 领域模型、领域服务、仓储接口、业务规则 |
| `infrastructure` | MySQL、Redis、MQ、外部服务访问、仓储实现 |
| `trigger` | 入站适配器，HTTP、Dubbo Provider、Job、MQ Listener |
| `app` | 启动装配层，Spring Boot 启动类和运行配置 |

推荐依赖方向：

```text
app -> trigger + infrastructure
trigger -> api + domain
infrastructure -> domain + common-types
domain -> common-types
api -> common-types
```

禁止依赖方向：

```text
domain -> infrastructure
domain -> trigger
domain -> Dubbo / MyBatis / Redis / Spring Web / Nacos
api -> domain / infrastructure / trigger
```

## 3. 学习协作规则

### 3.1 基本协作方式

本项目采用“用户主导实践，AI 导师审查引导”的方式推进。

要求：

1. 每一步先讲为什么，再讲怎么做，最后讲怎么验证。
2. 不直接一次性生成大量完整代码，除非用户明确要求。
3. 用户先动手实践，AI 负责检查、解释、纠偏和给出下一步小任务。
4. 如果用户卡住较久，AI 可以提供更具体的代码示例。
5. 每次只推进一个小阶段，不一次性展开过多内容。
6. 如果设计不合理，AI 要直接指出，并说明更好的方案。
7. 项目标准按企业级要求执行，不按简单学习 Demo 标准执行。
8. 每次回复先给一个简短小结，再进入结论、问题和下一步任务。
9. 业务相关任务要优先按真实生产要求思考，主动修正原设计里不合理的边界、职责和数据归属。

### 3.2 检查代码和结构时的规则

AI 检查项目前，需要先说明：

- 要看哪些文件
- 为什么要看这些文件
- 本次检查目标是什么

检查后输出顺序：

0. 当前进度小结
1. 结论
2. 发现的问题
3. 为什么是问题
4. 下一步任务
5. 验证方式

当前进度小结要求：

- 只用 1 到 3 句，简单概括这一步看到了什么、卡在哪里、下一步做什么。
- 重点是先把对话收拢，不要一上来就铺太长的分析。

### 3.3 Maven 依赖变更前置说明规则

如果某一步需要新增、删除或调整 Maven 依赖，AI 不能直接修改 POM。

必须先向用户说明：

- 本次为什么需要引入或调整依赖。
- 具体依赖坐标，包括 `groupId`、`artifactId`、版本来源、`scope`、`optional`。
- 父 POM 是否需要变化，例如是否新增 `properties`、`dependencyManagement`、`pluginManagement`。
- 哪些子模块 POM 需要新增依赖，分别为什么需要。
- 哪些模块明确不应该新增该依赖，尤其是 `api`、`domain`、`common-types` 是否需要保持纯净。
- 该依赖属于哪个技术边界，例如 MyBatis、Redis、Dubbo、Nacos、Spring Web、MQ。
- 有没有不引入该依赖的替代方案，以及为什么当前选择它。
- 引入后怎么验证依赖方向和编译结果。

原则：

```text
先讲依赖设计，再改 POM。

父 POM 负责版本管理和公共约束。
子模块 POM 只声明本模块真正需要的依赖。
domain / api / common-types 不能因为实现方便而被基础设施依赖污染。
```

### 3.4 每阶段验收规则

每个阶段都必须有明确验收标准。

源码级验收优先规则：

```text
每个小阶段完成后，AI 必须先做源码级验收，再做命令级验收。

源码级验收必须逐项检查本阶段要求实现的类、方法、配置、Mapper XML、依赖边界和模块归属。
不能只因为 Maven 编译成功、依赖方向检查无输出，就判断阶段完成。

如果本阶段目标是实现某个 Repository、Service、Controller、Provider、Job、Listener 等具体代码，
AI 必须打开实际源码，按方法逐项说明是否实现、实现逻辑是否符合业务语义、是否存在空实现、伪实现、TODO、错误兜底或边界泄漏。

命令级验收只能作为补充，用于确认编译、启动、接口调用或环境连通性。
结论顺序必须是：源码实现是否完成 -> 边界是否正确 -> 命令验证结果。
```

环境验收分工规则：

```text
如果当前是公司环境，AI 只做结构、代码、配置、依赖方向等文件级验收，不主动执行 Maven 编译、服务启动等构建命令。编译结果由用户在公司环境自行执行后反馈。

如果当前是家环境，AI 可以执行 Maven 编译、安装、服务启动等命令，并把命令结果纳入验收结论。
```

常见验收方式：

- 源码逐项检查本阶段目标是否真实实现
- 检查是否存在空实现、伪实现、TODO 或只编译不工作的代码
- `mvn clean compile`
- `mvn clean install`
- 服务可启动
- Nacos 可看到服务注册
- Dubbo RPC 可调用
- 接口可通过 HTTP 测试
- MySQL / Redis / MQ 状态可验证
- 代码依赖方向符合六边形架构

### 3.5 每阶段文档沉淀

每个阶段完成后，需要沉淀：

- 本阶段目标
- 改造前问题
- 改造后结构
- 关键设计决策
- 技术选型理由
- 踩坑记录
- 面试表达
- 遗留风险
- 下一阶段计划

阶段切换规则：

```text
每完成一个阶段，必须先完成阶段复盘文档，再进入下一阶段。

阶段复盘文档完成后，新的阶段必须开启新对话推进，避免上下文混杂。

如果用户提醒“阶段是否完成”或“需要复盘”，AI 必须先收口当前阶段，不继续推进新阶段任务。
```

### 3.6 最佳实践与亮点保留规则

本项目不是 Demo 迁移，也不是以最少代码跑通功能为目标。

每次迁移旧单体能力时，AI 必须同时做三件事：

1. 识别旧代码中值得保留的设计亮点。
2. 识别旧代码中不适合微服务拆分后继续照搬的问题。
3. 给出符合当前微服务边界、六边形架构和面试表达价值的改造方案。

必须保留和升级的项目亮点：

- 活动试算链路中的规则树 / 策略编排思想
- 优惠计算中的策略模式
- 领域层只依赖端口，不感知基础设施实现
- 标签能力通过 `ITagQueryPort` 接入，不直接访问标签表或 Redis bitmap
- DCC / 灰度 / 降级通过控制端口接入，不混入 Repository
- 缓存作为独立适配能力设计，不让 PO 或 Repository 过早承担缓存 key 职责

禁止行为：

```text
为了方便，把完整业务链路写成一个大 service 方法。
为了照搬旧代码，把 Spring @Service、ThreadPoolExecutor、Redis、Dubbo、MyBatis Mapper 带入 domain。
为了复用，把只服务某个业务的规则树框架、节点、业务模型塞进 common-types。
为了跑通，把旧单体中已经发现的边界问题继续迁入新微服务。
```

正确做法：

```text
旧业务语义要保留。
旧技术结构要审查后再决定是否保留。
旧项目亮点要在新架构下升级，而不是简单复制。
新实现必须能讲清楚：为什么这样拆、为什么这样编排、为什么这个边界更清晰。
```

### 3.7 回复格式约定

为了避免后续回复过于简略，统一补充以下约定：

1. 每次回复都要先给一个简短小结，说明当前进展和本次结论。
2. 下一步任务不能只写标题，要按点拆开，并尽量写清楚：
   - 要做什么
   - 为什么要做
   - 怎么做
   - 怎么验证
   - 这一步在业务上修正了什么不合理点
3. 涉及业务逻辑时，优先按真实生产中的要求写，不按学习 Demo 的最低实现写。
4. 如果原设计里有不合理边界，要明确指出，并给出修正方向，不要只给“能跑通”的方案。
5. 需要引用原代码位置时，只写“模块 / 包 / 类名”的短格式，不要再输出很长的完整包路径。
6. 代码位置示例：
   - `group-buy-activity-domain / service / trial / ActivityTrialRuleEngine`
   - `group-buy-activity-domain / model / valobj / GroupBuyActivityDiscountVO`
   - `xuele-group-buy-domain / service / trial / node / TagNode`
7. 如果下一步任务涉及多个点，每个点都要单独说明，不要合并成一段笼统描述。

## 4. 六边形架构约束

六边形架构关注的是依赖方向，而不是单纯的目录名称。

核心原则：

```text
领域层表达业务规则。
外部技术通过适配器接入。
依赖方向永远指向领域核心。
```

### 4.1 domain 层允许做什么

允许：

- 定义领域实体
- 定义值对象
- 定义领域服务
- 定义仓储接口
- 编排业务规则
- 抛出业务异常

不允许：

- 直接使用 MyBatis Mapper
- 直接使用 Redis / Redisson
- 直接使用 Dubbo 注解
- 直接使用 Spring MVC Controller
- 直接依赖 PO、DAO、RPC Client 实现

### 4.2 infrastructure 层职责

负责实现领域层定义的端口：

- DAO / Mapper
- PO
- MyBatis XML
- Redis 读写
- MQ 发送
- 外部 RPC 调用
- 仓储实现

### 4.3 trigger 层职责

负责接收外部输入：

- HTTP Controller
- Dubbo Provider
- MQ Listener
- 定时任务入口
- 命令行任务入口

### 4.4 app 层职责

负责启动和装配：

- Spring Boot 启动类
- `application.yml`
- 日志配置
- 服务端口配置
- 启动依赖聚合

app 层不写业务逻辑。

## 5. 微服务拆分原则

### 5.1 服务自治

每个微服务都应该拥有清晰的业务边界：

- 自己的业务模型
- 自己的数据归属
- 自己的缓存归属
- 自己的启动入口
- 自己的配置
- 自己的部署边界

### 5.2 数据归属

拆分服务时必须回答：

- 哪个服务拥有这张表？
- 哪个服务可以直接读写这张表？
- 其他服务如何访问这份数据？
- 是否通过 RPC？
- 是否通过事件同步？

例如 tag 服务：

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

这些表应该归 `group-buy-tag-service` 所有。

其他服务不能直接访问这些表，只能通过 `group-buy-tag-api` 暴露的 RPC 契约访问标签能力。

### 5.3 缓存归属

缓存也要有明确归属。

例如标签 bitmap：

```text
crowd:tag:bitmap:{tagId}
```

应该由 tag 服务维护。

activity / trade 等服务不能直接读写该 bitmap，只能通过 tag-service 查询用户是否命中标签。

### 5.4 RPC 契约

服务之间通过 API 契约通信。

原则：

- 消费方只依赖 provider 的 `api` jar
- 不依赖 provider 的 domain / infrastructure / app
- DTO 必须稳定、可序列化
- 接口需要考虑版本号、超时、异常语义

## 6. 技术选型表达要求

每引入一个技术组件，都必须能回答：

1. 为什么需要它？
2. 它解决了什么问题？
3. 不用它会怎样？
4. 它有什么风险？
5. 它在本项目中的边界是什么？
6. 面试时怎么讲？

示例：

### 6.1 Dubbo

使用原因：

- 内部服务之间需要高性能 RPC
- 契约清晰，适合 Java 服务间通信
- 支持服务治理能力，如超时、重试、负载均衡、版本分组

边界：

- Dubbo 注解只允许出现在 trigger 或 infrastructure 适配层
- domain 不感知 Dubbo

### 6.2 Nacos

使用原因：

- 服务注册发现
- 后续可承载配置中心能力

边界：

- 服务启动时向 Nacos 注册
- 消费方通过 Nacos 发现 provider
- 业务代码不直接依赖 Nacos API

### 6.3 Redis

使用原因：

- 高性能缓存
- bitmap 支持大规模标签命中判断
- 库存预占等高并发场景

边界：

- Redis key 要有清晰业务归属
- 不能多个服务随意读写同一类 key

### 6.4 Zookeeper

计划用途：

- 分布式锁
- 任务抢占
- 对比 Redis 锁与 Zookeeper 锁的差异

引入前必须说明：

- 具体解决哪个场景
- 为什么不用 Redis 锁
- 锁失败、超时、节点断开时怎么处理

## 7. 项目拆分阶段路线

重要边界：

```text
旧单体项目不作为后续改造对象。
旧单体只用于阅读、对照、迁移设计和必要的数据/SQL 参考。

后续访问 tag-service、调用 ITagQueryService、替换标签表/bitmap 直连逻辑，
都发生在新微服务项目继续拆 activity-service、trade-service 等服务时。

不要设置“改造旧单体主链路调用 tag-service”的阶段。
```

### 阶段 0：整体规划

目标：

- 梳理单体结构
- 确定服务拆分顺序
- 确定技术选型
- 确定新项目目录结构

### 阶段 1：group-buy-common

目标：

- 搭建公共基础 jar
- 完成 `group-buy-common-types`
- 只放稳定公共类型

验收：

```text
group-buy-common 可 mvn clean install
group-buy-common-types 无 Spring / MyBatis / Redis 等框架污染
```

### 阶段 2：group-buy-tag-service 骨架

目标：

- 搭建 tag 服务内部五层模块
- 确认 Maven 依赖方向
- 确认六边形结构

### 阶段 3：tag-service 真实迁移

目标：

- 迁移标签领域模型
- 迁移标签 DAO / PO / MyBatis XML
- 迁移 Redis bitmap 读写
- 提供 Dubbo 标签查询接口

当前验收边界：

```text
tag-service provider 侧完成即可收口。
真实 RPC 消费方验证后置到后续新微服务拆分阶段完成。
```

### 阶段 4：activity-service 拆分

目标：

- 活动配置
- 营销试算
- 活动规则树
- 活动缓存
- 作为新微服务消费者接入 tag-service
- 通过 Dubbo 调用 `ITagQueryService`
- 不直接访问 tag-service 拥有的标签表或标签 bitmap

阶段 4 底层要求：

```text
activity-service 迁移必须保留“活动试算规则树 + 优惠策略模式”这个项目亮点。

不能把旧规则树退化成 ActivityTrialRuleEngine 一个大方法。

可以重构旧规则树，但重构目标是：
1. domain 纯 Java，不加 Spring @Service。
2. 不依赖旧单体的 tree 框架，除非后续专门设计一个纯净、稳定、可复用的规则树基础包。
3. 不在 domain 引入 ThreadPoolExecutor、Dubbo、Redis、MyBatis、Nacos。
4. 标签判断只依赖 ITagQueryPort。
5. 活动数据读取只依赖 IActivityRepository。
6. 灰度、降级、试算控制只依赖 IActivityTrialControlPort。
7. 折扣计算通过 Map<String, IDiscountCalculateService> 路由。
```

推荐新规则树结构：

```text
ActivityTrialRuleEngine
  -> RootNode
  -> DataLoadNode
  -> TrialControlNode
  -> TagNode
  -> MarketNode
  -> EndNode
```

职责边界：

| 节点 | 职责 |
| --- | --- |
| `ActivityTrialRuleEngine` | 规则树入口，只创建上下文并调用根节点 |
| `RootNode` | 校验 `userId/goodsId/source/channel` 等试算入参 |
| `DataLoadNode` | 通过 `IActivityRepository` 加载商品活动关系、有效活动优惠、SKU |
| `TrialControlNode` | 通过 `IActivityTrialControlPort` 处理降级、灰度、切量 |
| `TagNode` | 通过 `ITagQueryPort` 判断用户是否命中标签 |
| `MarketNode` | 通过优惠策略 Map 路由 `ZJ/MJ/N/ZK` 并计算价格 |
| `EndNode` | 组装 `TrialBalanceEntity` |

旧单体规则树迁移原则：

```text
旧业务流程要参照。
旧技术依赖不照搬。
旧边界问题要在微服务迁移时修正。
```

验收：

```text
activity-service 只依赖 tag-service 的 api jar。
activity-service 不依赖 tag-service 的 domain / infrastructure / app。
activity-service 不直接访问 crowd_tags 相关表和 crowd:tag:bitmap:{tagId}。
真实业务接口中完成一次标签 RPC 调用验证。
```

### 阶段 5：trade-service 拆分

目标：

- 锁单
- 结算
- 退单
- 订单状态机
- 库存一致性
- 如交易链路需要标签判断，只通过 tag-service API 调用

### 阶段 6：settlement-service 拆分

目标：

- 结算规则
- 结算单据
- 结算状态
- 结算通知入口
- 与 trade-service 的边界划分

### 阶段 7：异步任务与 MQ 拆分

目标：

- 结算通知
- 退单通知
- 补偿任务
- 消息可靠性
- 消费幂等

### 阶段 8：分布式锁和一致性

目标：

- 引入 Zookeeper 或对比 Redis / Zookeeper 锁
- 处理任务抢占
- 处理并发锁单
- 处理库存恢复

### 阶段 9：配置中心与服务治理

目标：

- Nacos 配置中心
- 环境隔离
- 超时
- 重试
- 降级
- 健康检查

### 阶段 10：部署与可观测性

目标：

- Docker Compose
- 云服务器部署
- 日志规范
- Actuator 健康检查
- 指标监控
- 链路追踪

### 阶段 11：文档与面试表达

目标：

- 架构图
- 核心链路图
- 简历亮点
- 面试问答
- 踩坑复盘

## 8. 新对话提示词维护规则

旧版手册曾把“固定背景、协作规则、当前进度、当前任务”全部复制到一个超长模板里，后续每推进一个阶段都要重复修改大量内容。

现在统一改为三段式：

```text
固定背景：长期稳定，不频繁改。
当前进度快照：只维护阶段状态和关键文档索引。
当前任务：每次新对话按正在推进的小阶段单独填写。
```

原则：

- 固定背景不要重复写每个阶段的全部细节。
- 当前进度只写“已完成什么、文档在哪里、下一阶段是什么”。
- 当前任务只描述本次小阶段目标、检查范围、验收要求。
- 如果阶段完成，先更新当前进度快照，再开启新阶段。
- 不再复制上一阶段的完整长清单，详细内容以阶段复盘文档为准。

## 9. 新对话固定提示词模板

每次新开对话，先复制“固定背景”，再按需要补充“当前进度快照”和“当前任务”。

### 9.1 固定背景

```text
你是我的 Java 后端微服务架构导师和项目协作伙伴。

我正在把一个 Java 学习项目改造成企业级分布式微服务项目，用于秋招简历和面试。这个项目不是单纯跑通功能，而是要通过真实重构过程学习并沉淀：DDD、六边形架构、微服务拆分、Dubbo、Nacos、Redis、MySQL、MQ、分布式锁、任务调度、服务治理、配置治理、可观测性、部署运维、面试表达。

旧单体项目路径：
E:\Code\Code4Java\xuele-group-buy

新微服务项目路径：
E:\Code\Code4Java\group-buy-microservice

公司环境路径：
旧单体项目路径：D:\Code4J\xuele-group-buy
新微服务项目路径：D:\Code4J\group-buy-microservice

家环境路径：
旧单体项目路径：E:\Code\Code4Java\xuele-group-buy
新微服务项目路径：E:\Code\Code4Java\group-buy-microservice

项目边界：
旧单体项目保持不变，不作为后续改造对象。
旧单体只用于阅读、对照、迁移设计和必要的数据/SQL 参考。
后续服务间调用、访问 tag-service、接入 ITagQueryService，全部发生在新微服务项目继续拆分出的服务之间。

路径确认要求：
每次新对话开始时，如果我没有明确说明当前是公司环境还是家环境，而你需要读取、检查、修改项目文件，或者需要执行命令，你必须先问我：当前使用公司路径还是家路径？
在路径不确定时，不允许自行假设项目目录，也不允许直接使用某个默认路径继续操作。

编译验收分工要求：
如果当前是公司环境，你只做结构、代码、配置、依赖方向等文件级验收，不主动执行 Maven 编译、服务启动等构建命令，编译由我自己执行后反馈。
如果当前是家环境，你可以执行 Maven 编译、安装、服务启动等命令，并把命令结果纳入验收结论。

我的核心要求：
1. 请以教学和引导为主，不要直接替我写完所有代码。
2. 每一步都要先讲为什么，再讲我该怎么做，最后讲怎么验证。
3. 除非我明确要求你直接修改代码，或者我卡住很久，否则不要直接给我大段完整代码。
4. 你可以检查我的项目结构、pom、配置、代码，但检查前请说明你要看什么、为什么看。
5. 检查后先给结论，再给问题，再给下一步任务。
6. 每次只推进一个小阶段，不要一次性展开太多。
7. 要按企业级标准要求我，而不是学习 Demo 标准。
8. 如果我设计不合理，你要直接指出，并解释更好的方案。
9. 如果涉及六边形架构，要重点检查依赖方向：domain 不能依赖 Dubbo、MyBatis、Redis、Spring Web、Nacos 等外部技术。
10. 如果涉及微服务拆分，要重点检查服务边界、数据归属、缓存归属、RPC 契约、故障边界、部署边界。
11. 如果需要新增、删除或调整 Maven 依赖，请先说明具体依赖坐标、父 POM 是否变化、哪些子模块 POM 需要新增、哪些模块不应该新增，以及为什么这样设计；未经说明不要直接改 POM。
12. 每个阶段结束时，要帮我总结：我做了什么、为什么这么做、面试怎么讲、还有什么风险。
13. 回答使用中文，风格像老师带学生做真实企业项目。
14. 最佳实践优先，不以最小实现作为最终设计；如果某个方案只是为了快速跑通，但会损失项目亮点、架构表达或后续扩展性，必须明确指出并给出更好的方案。
15. 迁移旧单体时，不是复制粘贴，而是保留旧业务语义和设计亮点，同时修正旧代码中不适合微服务拆分后的边界问题。
16. 必须主动保留和升级项目亮点，例如规则树、责任链、策略模式、六边形端口、服务自治、数据归属、缓存归属、RPC 契约、配置治理、故障边界等。
17. 不能为了少写代码，把原本有业务表达价值的规则树、策略链或领域编排退化成一个大方法。

当前项目总体规划：
group-buy-microservice/
  group-buy-common/
    group-buy-common-types/
    后续可能扩展：
      group-buy-common-starter-web/
      group-buy-common-starter-dubbo/
      group-buy-common-starter-redis/

  group-buy-tag-service/
    group-buy-tag-api/
    group-buy-tag-domain/
    group-buy-tag-infrastructure/
    group-buy-tag-trigger/
    group-buy-tag-app/

  后续继续拆：
    group-buy-activity-service/
    group-buy-trade-service/
    group-buy-settlement-service/
    group-buy-job-service/ 或独立任务调度模块
```

### 9.2 当前进度快照

这段需要随着阶段推进维护。当前快照如下：

```text
截至 2026-06-08 当前进度：

1. 阶段 0：整体规划已完成，已确定按业务能力纵向拆分，新项目为独立微服务项目。
2. 阶段 1：group-buy-common 已完成，复盘文档：docs/microservice/04-stage-1-common-review.md。
3. 阶段 2：group-buy-tag-service 骨架已完成，复盘文档：docs/microservice/05-stage-2-tag-service-skeleton-review.md。
4. 阶段 3-1：tag-service API / Domain 已完成，检查点：docs/microservice/06-stage-3-1-tag-api-domain-checkpoint.md。
5. 阶段 3-2：tag-service Infrastructure / Repository 已完成，检查点：docs/microservice/07-stage-3-2-tag-infrastructure-repository-checkpoint.md。
6. 阶段 3-3：tag-service App 装配与启动验收已完成，检查点：docs/microservice/08-stage-3-3-tag-app-assembly-checkpoint.md。
7. 阶段 3-4：tag-service Dubbo Provider 暴露与 Nacos 注册已完成，检查点：docs/microservice/09-stage-3-4-tag-dubbo-provider-checkpoint.md。
8. 阶段 3 已正式完结：tag-service provider 侧迁移完成。
9. tag-service 当前具备独立启动、独立数据访问、Redis bitmap 查询、领域服务编排、Dubbo Provider 暴露和 Nacos 注册能力。
10. 当前不改旧单体主链路，不写临时 RPC Test；真实 RPC 消费验证放到后续新微服务拆分阶段。
11. 阶段 4：group-buy-activity-service 拆分已开始。
12. 阶段 4-1-1：activity-service infrastructure 包结构、PO、DAO 空接口、Mapper XML 基础 dataMap、ActivityRepository 空壳已完成。
13. 阶段 4-1-2：activity-service 自有表查询能力已完成，涉及 group_buy_activity、group_buy_discount、sc_sku_activity、sku。
14. 阶段 4-1-2 中已主动修正旧单体不合理点：sku 查询改为 goodsId + source + channel；有效活动查询增加 status + start_time/end_time；ActivityRepository 不再混入标签、Redis bitmap、DCC、缓存逻辑；PO 不放缓存 key。
15. 阶段 4-1-3A：优惠策略已完成。DirectReductionDiscountCalculator、FullReductionDiscountCalculator、FixedPriceDiscountCalculator、RateDiscountCalculator 均已实现，domain 不加 Spring 注解。
16. 家环境已执行过 activity-service `mvn -q -DskipTests compile`，通过。
17. 阶段 4-1-3B：activity-service 活动试算规则树节点迁移已完成，检查点：docs/microservice/10-stage-4-1-3B-activity-trial-rule-tree-nodes-checkpoint.md。
18. 阶段 4-1-3B 已引入 `group-buy-common-design` 纯 Java 规则树基础包，包含 `StrategyHandler`、`StrategyMapper`、`AbstractStrategyRouter`。
19. activity-domain 已新增 `ActivityTrialContext` 和 `AbstractActivityTrialSupport`，用 `AbstractActivityTrialSupport` 固定活动试算规则树泛型，避免每个节点重复声明泛型。
20. activity-domain 已完成 `RootNode`、`DataLoadNode`、`TrialControlNode`、`TagNode`、`MarketNode`、`EndNode` 六个节点。
21. `GroupBuyActivityDiscountVO` 已补充 `isVisible()` / `isEnable()`；`group-buy-common-types` 已新增 `StringUtils.isBlank()`，避免为了字符串判空引入 Apache Commons。
22. `ResponseCode` 已新增活动试算相关错误码：`NO_DISCOUNT_CALCULATOR`、`NO_ACTIVITY_MARKET_CONFIG`、`ACTIVITY_TRIAL_DOWNGRADE`、`ACTIVITY_TRIAL_GRAY_RANGE_BLOCKED`。
23. `TagNode` 已按真实业务拆分活动门禁和优惠资格：`group_buy_activity.tag_id` 控制活动可见/可参与，`group_buy_discount.tag_id` 控制 TAG 专享优惠资格。
24. 本阶段主动修正旧单体不合理边界：不迁移 `ErrorNode` 空兜底节点，缺失营销配置由负责节点直接抛业务异常；不迁移旧 `AbstractMultiThreadStrategyRouter`，不让 domain 父类持有线程池。
25. activity-domain 仍保持纯净，只依赖 `common-types`、`common-design`、Lombok；没有引入 Spring、MyBatis、Redis、Dubbo、Nacos、线程池等外部技术。
26. 当前是公司环境，本阶段只完成源码级验收，未执行 Maven 编译、服务启动或 RPC 调用。
27. 阶段 4-1-3C 已开始：activity-domain 已完成规则树入口和领域服务入口。
28. activity-domain 已新增 `IActivityTrialRuleEngine`、`ActivityTrialRuleEngine`、`IActivityTrialService`、`ActivityTrialService`；规则树入口只持有 `RootNode`，领域服务只依赖 `IActivityTrialRuleEngine`。
29. `ActivityTrialContext`、`AbstractActivityTrialSupport` 已调整到 `service/trial/engine` 包下，`engine` 与 `node` 并列表达规则树支撑结构，不再挂在节点子包下。
30. activity-infrastructure 已完成 `ActivityTrialControlPort` 基础实现，默认不降级、默认全量进入灰度；实现位于 infrastructure adapter，不污染 domain。
31. activity-infrastructure POM 已新增 `dubbo-spring-boot-starter` 和 `group-buy-tag-api` 依赖；activity 父 POM 已管理 `group-buy-tag-api` 版本。
32. tag-api 依赖问题已排查到环境层面：`group-buy-tag-api` target jar 中存在 `cn/xuele/api/tag/ITagQueryService.class`，但公司 Maven 当前为 Maven 3.3.9 + JDK 8，且实际本地仓库 `D:\Work\Maven\maven\repository` 未安装 `group-buy-tag-api`、`group-buy-tag-service` 父 POM、`group-buy-common-types` 等当前微服务产物。该问题暂时挂起，回家后使用 JDK 21 环境重新 install common 与 tag-service 后继续。
33. 下一步回家继续阶段 4-1-3C：先恢复 Maven/JDK 21 与本地仓库依赖解析，再实现 `ITagQueryPort` Dubbo adapter，之后进入 app 层 `DomainServiceConfig` 手动装配。
```

### 9.2.1 阶段 4 当前代码快照

家环境新项目路径：

```text
E:\Code\Code4Java\group-buy-microservice\group-buy-activity-service
```

公司环境新项目路径：

```text
D:\Code4J\group-buy-microservice\group-buy-activity-service
```

当前已完成源码：

```text
group-buy-activity-domain
  adapter/repository/IActivityRepository.java
  adapter/port/ITagQueryPort.java
  adapter/port/IActivityTrialControlPort.java
  model/entity/MarketProductEntity.java
  model/entity/TrialBalanceEntity.java
  model/valobj/GroupBuyActivityDiscountVO.java
  model/valobj/SCSkuActivityVO.java
  model/valobj/SkuVO.java
  model/valobj/TagScopeEnumVO.java
  model/valobj/DiscountTypeEnum.java
  model/valobj/DiscountMarketPlanEnum.java
  service/discount/IDiscountCalculateService.java
  service/discount/AbstractDiscountCalculateService.java
  service/discount/impl/DirectReductionDiscountCalculator.java
  service/discount/impl/FullReductionDiscountCalculator.java
  service/discount/impl/FixedPriceDiscountCalculator.java
  service/discount/impl/RateDiscountCalculator.java
  service/trial/IActivityTrialService.java
  service/trial/ActivityTrialService.java
  service/trial/engine/IActivityTrialRuleEngine.java
  service/trial/engine/ActivityTrialRuleEngine.java
  service/trial/engine/ActivityTrialContext.java
  service/trial/engine/AbstractActivityTrialSupport.java
  service/trial/node/RootNode.java
  service/trial/node/DataLoadNode.java
  service/trial/node/TrialControlNode.java
  service/trial/node/TagNode.java
  service/trial/node/MarketNode.java
  service/trial/node/EndNode.java

group-buy-common
  group-buy-common-types/common/StringUtils.java
  group-buy-common-types/enums/ResponseCode.java
  group-buy-common-design/framework/tree/StrategyHandler.java
  group-buy-common-design/framework/tree/StrategyMapper.java
  group-buy-common-design/framework/tree/AbstractStrategyRouter.java

group-buy-activity-infrastructure
  dao/po/GroupBuyActivity.java
  dao/po/GroupBuyDiscount.java
  dao/po/SCSkuActivity.java
  dao/po/Sku.java
  dao/IGroupBuyActivityDao.java
  dao/IGroupBuyDiscountDao.java
  dao/ISCSkuActivityDao.java
  dao/ISkuDao.java
  adapter/repository/ActivityRepository.java
  adapter/port/ActivityTrialControlPort.java
  resources/mybatis/mapper/group_buy_activity_mapper.xml
  resources/mybatis/mapper/group_buy_discount_mapper.xml
  resources/mybatis/mapper/sc_sku_activity_mapper.xml
  resources/mybatis/mapper/sku_mapper.xml
```

当前明确未完成：

```text
app 层 DomainServiceConfig 尚未创建。
activity-app 启动装配尚未进入本阶段。
ITagQueryPort 的 Dubbo infrastructure adapter 尚未创建。
公司 Maven/JDK 环境暂未解决：当前公司命令行 Maven 为 3.3.9 + JDK 8，实际本地仓库 `D:\Work\Maven\maven\repository` 未安装当前微服务的 common/tag-api 产物，导致 activity-infrastructure 暂时解析不到 `cn.xuele.api.tag.ITagQueryService`。
activity-api 尚未定义对外试算 RPC 契约和 DTO。
trigger 尚未提供 HTTP/Dubbo 入站适配器。
DataLoadNode 暂未做异步多线程优化；后续只作为性能优化单独设计，不直接搬旧单体多线程父类。
```

### 9.2.2 下一阶段新对话接力任务

如果在新对话继续，当前任务是：

```text
【当前阶段】
阶段 4-1-3C：activity-service 活动试算规则树入口、领域服务与 app 装配。

【本次目标】
在不污染 domain 的前提下，把已经完成的规则树节点串成可调用的活动试算领域能力，并在 app 层完成手动装配。

本阶段只做规则树入口、领域服务、app 装配和必要端口适配设计。
不要展开 activity-api / trigger 对外接口，也不要进入交易服务迁移。

【优先检查文件】
1. group-buy-activity-domain/src/main/java/cn/xuele/activity/domain/service/trial/*
   目的：确认 `IActivityTrialService`、`ActivityTrialService` 已存在，领域服务只依赖规则树入口。

2. group-buy-activity-domain/src/main/java/cn/xuele/activity/domain/service/trial/engine/*
   目的：确认 `IActivityTrialRuleEngine`、`ActivityTrialRuleEngine`、`ActivityTrialContext`、`AbstractActivityTrialSupport` 包结构和职责正确。

3. group-buy-activity-domain/src/main/java/cn/xuele/activity/domain/service/trial/node/*
   目的：确认节点构造器依赖关系和路由顺序，避免 app 装配时接错链路。

4. group-buy-activity-app
   目的：确认 app 层是否已有启动类、配置类和装配位置；domain 节点不能加 Spring 注解。

5. group-buy-activity-infrastructure/src/main/java/cn/xuele/activity/infrastructure/adapter
   目的：确认 `IActivityTrialControlPort` 和 `ITagQueryPort` 的实现位置，避免端口实现写到 domain。

6. group-buy-tag-api/src/main/java/cn/xuele/api/tag/*
   目的：如果本阶段要实现 `ITagQueryPort` Dubbo adapter，必须确认 activity-service 只依赖 tag-service 的 api 契约。

7. 家环境 Maven/JDK 与本地仓库
   目的：确认使用 JDK 21，且 `group-buy-common`、`group-buy-tag-service` 已成功 install 到当前 Maven 本地仓库。

【下一步小任务】
1. 回家后先恢复 Maven/JDK 21 与本地仓库依赖解析。
   - 要做什么：确认 `mvn -version` 使用 JDK 21，并重新 install `group-buy-common`、`group-buy-tag-service`。
   - 为什么做：公司环境当前 Maven 为 3.3.9 + JDK 8，且实际本地仓库缺少 `group-buy-tag-api` 和 `group-buy-common-types`，导致 activity-infrastructure 找不到 `ITagQueryService`。
   - 怎么做：在家环境分别对 `group-buy-common`、`group-buy-tag-service` 执行 `mvn clean install -DskipTests`，再刷新 activity-service Maven。
   - 怎么验证：activity-infrastructure 能正常 import `cn.xuele.api.tag.ITagQueryService`。
   - 业务修正点：先保证跨服务契约 jar 可解析，再写 RPC adapter，避免把依赖问题误判成代码问题。

2. 完成 `ITagQueryPort` Dubbo adapter。
   - 要做什么：新增 `cn.xuele.activity.infrastructure.adapter.port.TagQueryPort`，实现 domain 的 `ITagQueryPort`。
   - 为什么做：`TagNode` 只应该依赖标签查询端口，不能直接依赖 tag-service 表、Redis bitmap 或内部模块。
   - 怎么做：在 adapter 内使用 `@DubboReference(check = false)` 持有 `ITagQueryService` 远程代理，调用 `matchCrowdTag(TagQueryRequestDTO)` 后解析 `Response<TagQueryResponseDTO>`。
   - 怎么验证：`TagQueryPort` 位于 infrastructure；activity-domain 不依赖 `group-buy-tag-api`；RPC 返回失败或 data 为空时抛 `AppException`，不能直接兜底为 `false`。
   - 业务修正点：正式把旧单体活动直接查标签数据，升级为 activity-service 通过 tag-service RPC 契约消费标签能力。

3. 新建 app 层 `DomainServiceConfig`。
   - 要做什么：在 app 层手动装配折扣策略、节点链路、规则树入口和领域服务。
   - 为什么做：domain 保持纯 Java，不加 `@Service` / `@Component`，对象生命周期交给 app 层。
   - 怎么做：按 `EndNode -> MarketNode -> TagNode -> TrialControlNode -> DataLoadNode -> RootNode -> ActivityTrialRuleEngine -> ActivityTrialService` 顺序创建 Bean；折扣策略 Map 使用 `IDiscountCalculateService.marketPlan()` 作为 key。
   - 怎么验证：domain 仍无 Spring 注解；策略 Map 的 key 必须是 `ZJ/MJ/N/ZK`，不能依赖 BeanName 偶然匹配。
   - 业务修正点：把规则树编排和 Spring 装配隔离开，避免领域模型被框架污染。

4. 异步多线程暂不进入本阶段实现。
   - 要做什么：不要复制旧单体 `AbstractMultiThreadStrategyRouter`。
   - 为什么做：旧实现把线程池能力塞进规则树父类，会让 domain 背上技术调度职责。
   - 怎么做：先完成同步规则树闭环；后续如果要优化 DataLoadNode，只优化“查询 SKU”和“查询活动绑定 -> 查询活动优惠”这两条数据加载路径。
   - 怎么验证：domain 中不出现 `ThreadPoolExecutor`、`CompletableFuture`、线程池配置。
   - 业务修正点：把性能优化和业务规则迁移解耦，避免为了并发查询破坏六边形边界。

【严格禁止】
1. 不把完整试算链路写进 ActivityTrialRuleEngine 一个大方法。
2. domain 不加 @Service / @Component。
3. domain 不依赖 MyBatis / Redis / Dubbo / Spring Web / Nacos。
4. TagNode 不访问 crowd_tags、crowd_tags_detail、Redis bitmap。
5. DataLoadNode 不使用 DAO/Mapper，只使用 IActivityRepository。
6. TrialControlNode 不直接读取配置中心实现，只使用 IActivityTrialControlPort。
7. 不把旧单体 `ErrorNode` 单独迁回规则树。
8. 不把旧单体多线程规则树父类直接迁到 common-design 或 domain。
```

公司环境验收提醒：

```text
如果当前是公司环境，AI 只做源码、结构、依赖边界验收，不主动执行 Maven 编译。
编译由用户在公司环境自行执行后反馈。
```

### 9.3 当前任务模板

每次新阶段只补这段，不要复制上一阶段完整长清单。

```text
【当前阶段】
我现在要做：【填写阶段名称，例如 阶段 4：group-buy-activity-service 拆分】。

【我当前需要完成的事情】
1. 请先说明本次要检查哪些文件、为什么检查、本次检查目标是什么。
2. 先给一个简短小结，再给结论和问题。
3. 每次只推进一个小阶段。
4. 下一步任务必须按点拆开，并且每个点都要写清楚：要做什么、为什么、怎么做、怎么验证。
5. 如果任务涉及业务逻辑，请按真实生产要求来写，主动修复原来不合理的边界、数据归属和职责混杂问题。
6. 如果涉及 Maven 依赖，先讲依赖设计，再改 POM。
7. 如果涉及六边形架构，重点检查 domain 是否保持纯净。
8. 如果涉及服务间调用，重点检查消费者只依赖 provider 的 api jar。
9. 不要修改旧单体项目主链路；旧单体只作为阅读和迁移参考。
10. 阶段完成前先做源码级验收，再做命令级验收。
11. 必须按最佳实践推进，保留并升级旧项目亮点，不能为了最小实现牺牲规则树、策略模式、服务边界和面试表达价值。

【我遇到的问题】
【填写当前卡点或目标，例如：需要先从旧单体阅读活动相关代码，判断 activity-service 的服务边界和表归属。】

【我的要求】
如果我在新对话里没有明确说明当前是公司环境还是家环境，而你需要读取、检查、修改项目文件，或者需要执行命令，请必须先问我当前使用公司路径还是家路径，不要自行假设路径。
如果当前是公司环境，请只做结构、代码、配置、依赖方向等文件级验收，不要主动执行 Maven 编译、服务启动等构建命令；编译由我自己执行后反馈。
如果当前是家环境，你可以执行 Maven 编译、安装、服务启动等命令，并把命令结果纳入验收结论。
如果本阶段需要新增、删除或调整 Maven 依赖，请先说明具体依赖坐标、父 POM 是否变化、哪些子模块 POM 需要新增、哪些模块不应该新增，以及为什么这样设计；不要直接改 POM。
请始终记住：最佳实践优先，保留项目亮点，主动挖掘新亮点；迁移不是照搬，必须修正旧代码中不合理的边界。
请你先不要直接写完整代码。
请你先说明要检查哪些文件、为什么检查、检查目标是什么。
检查后请按顺序输出：当前进度小结 -> 依赖设计是否合理 -> 本阶段源码实现是否完成 -> 边界是否正确 -> 命令验证是否需要执行 -> 剩余问题 -> 下一步小任务。
每一步请先讲为什么，再讲我该怎么做，最后讲怎么验证。
```

## 10. 每阶段复盘模板

每个阶段完成后，可以要求 AI 按下面格式复盘。

```text
请帮我做本阶段复盘，格式如下：

1. 本阶段完成了什么
2. 原来有什么问题
3. 我们为什么这样设计
4. 涉及哪些技术点
5. 企业级体现在哪里
6. 面试官可能怎么问
7. 我应该怎么回答
8. 当前还遗留什么风险
9. 下一阶段应该做什么
```

## 11. 面试表达总原则

面试中不要只说“我用了什么技术”，而要说清楚：

```text
原来有什么问题
为什么这个问题需要解决
我做了什么设计
我为什么这样选型
改造后带来了什么收益
过程中踩了什么坑
还有什么可优化点
```

推荐表达结构：

```text
背景 -> 问题 -> 方案 -> 落地 -> 验证 -> 风险 -> 后续优化
```

示例：

```text
原项目是一个按技术层拆分的单体 DDD 项目，适合学习领域建模，但随着业务复杂度上升，标签、活动、交易、结算等领域边界开始混在一起。

我在微服务改造中采用按业务能力纵向拆分的方式，每个服务独立工程，内部继续保持六边形架构。服务之间只通过 api 契约和 Dubbo 通信，不共享数据库访问层和领域实现。

以 tag-service 为例，crowd_tags 相关表和标签 bitmap 都归标签服务所有，activity-service 只保存 tagId，需要判断用户是否命中标签时，通过 Dubbo 调用 tag-service，而不是直接访问标签表或 Redis bitmap。
```

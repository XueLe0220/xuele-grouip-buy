# 阶段 2 复盘：group-buy-tag-service 骨架

## 1. 本阶段完成了什么

本阶段完成了 `group-buy-tag-service` 的五层模块骨架验收。

当前模块结构为：

```text
group-buy-tag-service/
  group-buy-tag-api/
  group-buy-tag-domain/
  group-buy-tag-infrastructure/
  group-buy-tag-trigger/
  group-buy-tag-app/
```

Maven 依赖方向已经整理为：

```text
app -> trigger + infrastructure
trigger -> api + domain
infrastructure -> domain
domain -> common-types
api -> common-types
```

同时完成了以下清理和校验：

- `api` 已依赖 `group-buy-common-types`
- `domain` 已依赖 `group-buy-common-types`
- `api` 本地 `Response<T>` 已删除，改为使用 common-types 中的统一响应模型
- `domain` 没有依赖 Dubbo、MyBatis、Redis、Spring Web、Nacos 等外部技术
- `xxx / yyy` 脚手架领域目录已清理
- `frame_case_mapper.xml` 脚手架 mapper 已清理
- `application-dev.yml` 已指向 `group_buy_tag`
- 初始化 SQL 已体现 `group_buy_tag`、`group_buy_activity`、`group_buy_trade` 分库
- 公司环境编译由用户自行执行并反馈通过，AI 不主动执行 Maven 编译

## 2. 原来有什么问题

最初的 `group-buy-tag-service` 只有目录骨架，五个子模块的 `pom.xml` 基本为空，依赖方向还没有建立。

同时存在脚手架遗留内容：

- 本地 `Response<T>`
- `xxx / yyy` 包
- `frame_case_mapper.xml`
- `xfg_frame_archetype` 数据库配置
- 模板化 dev-ops 资源

这些内容如果不清理，会让后续迁移时难以区分真实业务资产和模板资产。

## 3. 我们为什么这样设计

tag-service 是第一个被拆出来的业务微服务，需要先把架构边界立住，再迁移真实业务代码。

本阶段的核心不是功能跑通，而是确定依赖方向：

- `api` 只表达对外契约
- `domain` 只表达业务规则和领域端口
- `infrastructure` 负责 MySQL、Redis、MQ 等外部资源适配
- `trigger` 负责 HTTP、Dubbo Provider、Job、MQ Listener 等入口
- `app` 负责启动和装配

这样可以保证后续迁移时，不会把 MyBatis Mapper、Redis Client、Dubbo 注解直接带进领域层。

## 4. 涉及哪些技术点

- Maven 多模块依赖管理
- DDD 分层
- 六边形架构依赖方向
- 公共类型依赖
- 微服务数据归属
- Docker MySQL 初始化
- MySQL 字符集和 SQL 文件编码

## 5. 企业级体现在哪里

本阶段体现了几个企业级标准：

- 先验收服务骨架，再迁移业务代码
- 依赖方向先于功能实现
- 业务服务使用自己的数据库 `group_buy_tag`
- 通过 SQL 脚本表达数据库归属和初始化过程
- 使用 tag 专属数据库账号，而不是业务服务直接使用 root
- 公司环境和家环境采用不同验收分工，避免复杂公司环境影响协作节奏

## 6. 面试官可能怎么问

面试官可能会问：

- 为什么 tag-service 要拆成五个模块？
- 为什么 domain 不能依赖 MyBatis、Redis、Dubbo？
- api 模块为什么不能依赖 domain？
- app 层为什么只做启动和装配？
- tag-service 的表归谁所有？
- SQL 初始化时遇到编码问题怎么排查？

## 7. 我应该怎么回答

可以这样回答：

```text
我在拆 tag-service 时没有直接搬业务代码，而是先搭建五层模块并整理 Maven 依赖方向。

api 只放对外契约，domain 放领域模型、领域服务和仓储接口，infrastructure 实现 MySQL、Redis 等外部资源适配，trigger 放 HTTP、Dubbo Provider、Job 等入口，app 只做启动和装配。

这样做的目的是保证依赖方向始终指向领域核心，避免领域层直接感知 MyBatis、Redis、Dubbo、Nacos 等外部技术。

数据上，crowd_tags、crowd_tags_detail、crowd_tags_job 归 tag-service 所有，其他服务不能直接访问这些表，后续需要通过 tag-service 暴露的 API 或 Dubbo 契约访问标签能力。
```

## 8. 当前还遗留什么风险

- `application-dev.yml` 中需要确认 `tag_user` 使用的是 tag 专属密码，而不是 root 密码。
- `docs/dev-ops/mysql/sql/xfg-frame-archetype.sql` 仍属于模板遗留资源，后续需要清理或归档。
- 当前还没有迁移真实 tag 业务代码，领域模型和接口契约仍未落地。
- MyBatis、Redis、Dubbo 依赖后续引入时，需要继续保持不污染 domain。
- SQL 文件上传服务器时必须保持 UTF-8 编码，避免中文初始化数据导入失败。

## 9. 下一阶段应该做什么

下一阶段进入阶段 3：tag-service 真实迁移。

阶段 3 不应一上来直接复制代码，而是先做迁移清单设计：

- 旧单体中哪些类属于 tag 领域模型
- 哪些 DAO、PO、Mapper XML 属于 tag-service 的基础设施层
- Redis bitmap 的 key 和读写逻辑在哪里
- 哪些 HTTP、Job、RPC 入口应该进入 trigger
- 对外 Dubbo 契约应该如何设计

阶段 3 应在新对话中开启。

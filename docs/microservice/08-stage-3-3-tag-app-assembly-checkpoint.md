# 阶段 3-3：tag-service App 装配与启动验收检查点

## 1. 本阶段目标

阶段 3-3 的目标是完成 `group-buy-tag-service` 的 app 层装配与启动验收。

本阶段不进入 Dubbo Provider、Nacos 注册发现和 RPC 调用，只聚焦：

```text
让 tag-service app 能作为真正的 Spring Boot 应用启动。
让 app 正确装配 domain、infrastructure、trigger。
确认 MyBatis、Redis、Repository、领域服务 Bean 的装配链路完整。
```

本阶段完成后，tag-service 已具备最小可启动能力，后续才能继续进入 Dubbo / Nacos。

## 2. 本阶段完成了什么

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

当前完成范围：

```text
group-buy-tag-service/group-buy-tag-app
group-buy-tag-service/group-buy-tag-infrastructure
```

已完成内容：

- 新增 `TagApplication` 作为 Spring Boot 启动类。
- 新增 `DomainServiceConfig`，由 app 层显式装配 domain 服务。
- `group-buy-tag-app` 显式引入 `spring-boot-starter-web`。
- `group-buy-tag-app` 新增 `spring-boot-maven-plugin`。
- 修正 `application-dev.yml` 中 `spring.datasource.hikari` 与 `spring.datasource.type` 的层级。
- 补齐 Redis 密码配置，适配当前 Docker Redis 的 `requirepass`。
- 将 dev 默认 Redis host 调整为 `127.0.0.1`，适配本地宿主机启动应用、Docker 暴露端口的场景。
- 用户已在公司环境完成编译与启动验证，tag-service 可以正常启动。

## 3. Spring Boot 启动类验收

当前启动类：

```text
group-buy-tag-app/src/main/java/cn/xuele/tag/TagApplication.java
```

包路径：

```text
cn.xuele.tag
```

验收结论：

```text
启动类位于 cn.xuele.tag 根包下。
Spring Boot 默认组件扫描可以覆盖 app、trigger、infrastructure、domain 下的 cn.xuele.tag.* 包。
```

为什么这样设计：

```text
多模块 Maven 项目运行时会把依赖模块放入 classpath。
Spring Boot 的组件扫描按包路径工作，而不是按 Maven 模块目录工作。
因此启动类放在统一根包 cn.xuele.tag 下，可以扫描到依赖模块中的 Spring Bean。
```

## 4. Domain 服务装配验收

当前装配类：

```text
group-buy-tag-app/src/main/java/cn/xuele/tag/config/DomainServiceConfig.java
```

装配方式：

```java
@Bean
public ITagService tagService(ITagRepository tagRepository) {
    return new TagService(tagRepository);
}
```

验收结论：

```text
ITagService 已由 app 层装配为 Spring Bean。
TagService 仍然是纯 Java 领域服务，没有引入 Spring 注解。
```

为什么不直接在 `TagService` 上加 `@Service`：

```text
domain 层表达业务规则，不应该依赖 Spring、MyBatis、Redis、Dubbo、Nacos 等外部技术。
如果在 TagService 上加 @Service，domain 就会引入 Spring 框架依赖。
这会让领域模型与运行时框架绑定，不利于单测、迁移和保持六边形架构边界。
```

当前做法的好处：

- domain 层保持纯净。
- app 层负责 Spring 装配，符合启动装配层职责。
- `TagService` 可以在单元测试中直接 `new TagService(mockRepository)`。
- 后续如果更换装配框架，领域服务代码不需要跟着改。

## 5. Infrastructure Bean 装配验收

当前基础设施 Bean：

```text
TagRepository
RedisClientConfig
RedisClientConfigProperties
ICrowdTagsDao
ICrowdTagsDetailDao
ICrowdTagsJobDao
```

验收结论：

```text
TagRepository 通过 @Repository 被 Spring 管理。
RedisClientConfig 通过 @Configuration 创建 RedissonClient。
RedisClientConfigProperties 绑定 redis.sdk.config 配置。
三个 DAO 通过 @Mapper 被 MyBatis 管理。
```

装配链路：

```text
TagApplication
  -> DomainServiceConfig
      -> ITagService / TagService
          -> ITagRepository / TagRepository
              -> MyBatis DAO
              -> RedissonClient
```

这说明 app 不只是有配置文件，而是已经能把领域服务和基础设施适配器装配成完整运行时对象图。

## 6. MyBatis 配置验收

当前配置：

```yaml
mybatis:
  mapper-locations: classpath:/mybatis/mapper/*.xml
  config-location: classpath:/mybatis/config/mybatis-config.xml
```

Mapper XML 所在模块：

```text
group-buy-tag-infrastructure/src/main/resources/mybatis/mapper
```

MyBatis config 所在模块：

```text
group-buy-tag-app/src/main/resources/mybatis/config/mybatis-config.xml
```

验收结论：

```text
app 依赖 infrastructure 后，infrastructure 的 Mapper XML 会进入运行时 classpath。
mapper-locations 能扫描到 crowd_tags、crowd_tags_detail、crowd_tags_job 对应 XML。
DAO 接口与 XML namespace 已在阶段 3-2 对齐。
```

## 7. Redis 配置验收

当前 `application-dev.yml`：

```yaml
redis:
  sdk:
    config:
      mode: standalone
      password: ${REDIS_PASSWORD:xuele_redis_123456}
      standalone:
        host: ${REDIS_HOST:127.0.0.1}
        port: ${REDIS_PORT:16379}
```

当前 Docker Redis 配置：

```text
容器名：xuele-redis
端口映射：16379:6379
密码：xuele_redis_123456
```

本阶段踩坑：

```text
Redis 容器启用了 requirepass，但应用配置中最初没有配置 password。
同时 dev 配置默认连接远程 124.211.233.121:16379，导致本地启动时连接超时。
```

修正后：

```text
本地宿主机启动 Java 应用时，默认连接 127.0.0.1:16379。
Redisson 使用 xuele_redis_123456 完成 Redis 认证。
如果以后 app 也放入 Docker 网络，则通过环境变量改为 REDIS_HOST=xuele-redis、REDIS_PORT=6379。
```

## 8. Maven 依赖验收

当前 app 依赖方向：

```text
group-buy-tag-app
  -> spring-boot-starter-web
  -> group-buy-tag-trigger
  -> group-buy-tag-infrastructure
```

为什么 app 要显式引入 `spring-boot-starter-web`：

```text
app 是启动装配层，应该显式拥有 Spring Boot Web 启动能力。
不能依赖 infrastructure 的 MyBatis / Redisson starter 间接传递出 SpringApplication。
```

为什么加入 `spring-boot-maven-plugin`：

```text
app 是最终可启动模块，需要支持打包为可执行 Spring Boot jar。
```

未污染模块：

```text
group-buy-common-types
group-buy-tag-api
group-buy-tag-domain
```

验收结论：

```text
Spring Boot Web 启动能力只进入 app。
MyBatis、Redis、Redisson 仍只属于 infrastructure。
domain 没有引入 Spring 注解或 Spring 依赖。
```

## 9. 命令级验收

当前是公司环境，AI 不主动执行 Maven 编译或服务启动。

用户已反馈：

```text
编译通过。
Redis 密码和 host 修正后，tag-service 已经可以启动。
```

验收结论：

```text
阶段 3-3 命令级启动验收通过。
```

## 10. 为什么这样设计

阶段 3-3 的核心不是“让 Spring Boot 能跑”，而是确认微服务启动装配边界是否清晰。

设计原则：

```text
app 负责启动和装配。
domain 负责业务规则。
infrastructure 负责技术实现。
trigger 负责入站入口。
```

因此：

- 启动类属于 app。
- 配置文件属于 app。
- domain 服务由 app 配置类装配。
- Redis / MyBatis 细节留在 infrastructure。
- app 聚合 trigger + infrastructure，但不写业务逻辑。

## 11. 企业级体现在哪里

本阶段体现的企业级标准：

- 不只依赖 Maven 编译结果判断完成，而是检查启动类、扫描路径、Bean 装配和配置绑定。
- domain 不为了图方便加 `@Service`，而是由 app 装配领域服务。
- app 显式声明启动依赖，避免依赖传递带来的隐式启动能力。
- Redis 密码、host、端口通过配置和环境变量管理，避免硬编码单一环境。
- 识别 Docker 宿主机访问和容器网络访问的区别。
- 在进入 Dubbo / Nacos 前先保证本服务最小启动闭环。

## 12. 当前还遗留什么风险

### 12.1 Redis 密码默认值暴露

当前 dev 配置中保留了默认密码：

```text
xuele_redis_123456
```

风险：

```text
开发环境可以接受，但生产环境不能把真实密码写入仓库。
```

后续方案：

```text
生产环境通过环境变量、Nacos 配置中心或密钥管理系统注入。
```

### 12.2 dev 配置仍绑定具体环境

当前 MySQL 和 Redis dev 配置仍带有具体环境地址。

风险：

```text
公司环境、家环境、Docker 环境、云服务器环境的连接方式不同。
```

后续方案：

```text
阶段 9 配置中心治理时统一处理环境隔离和配置外置。
```

### 12.3 目前还没有业务入口

当前服务可以启动，但 trigger 层还没有 HTTP Controller、Dubbo Provider、Job 或 MQ Listener。

风险：

```text
启动成功只能说明装配闭环成立，还不能说明标签能力已经对外可调用。
```

后续方案：

```text
下一小阶段进入 Dubbo Provider，暴露 ITagQueryService 查询契约。
```

## 13. 面试表达

可以这样表达本阶段：

```text
在 tag-service 的 app 装配阶段，我没有简单地把 @Service 注解加到 domain 的领域服务上，而是让 app 层通过配置类显式装配领域服务。

这样做的原因是 domain 层应该保持业务规则纯净，不依赖 Spring。Spring Boot 启动类、配置文件、Bean 装配这些运行时细节都放在 app 层。

同时，MyBatis、Redis、Redisson 的实现细节留在 infrastructure，app 只负责把这些模块装配起来。

本阶段还处理了一个真实启动问题：Redis 容器开启了 requirepass，但应用最初没有配置密码，并且默认 host 指向远程地址，导致 RedissonClient 初始化失败。修正后通过环境变量形式管理 Redis host、port、password，最终完成了 tag-service 的最小启动闭环。
```

## 14. 阶段 3-3 验收结论

源码级结论：

```text
阶段 3-3：tag-service app 装配源码级验收通过。
```

边界结论：

```text
app 层负责启动与装配。
domain 未被 Spring、MyBatis、Redis、Redisson、Dubbo、Nacos 污染。
infrastructure Bean 能被 app 扫描和装配。
```

命令级结论：

```text
用户已在公司环境完成编译和启动验证，tag-service 可以启动。
```

## 15. 下一阶段应该做什么

阶段 3-3 完成后，下一步进入阶段 3-4。

建议阶段 3-4 目标：

```text
实现 tag-service Dubbo Provider。
让 group-buy-tag-trigger 通过 ITagQueryService 暴露标签查询能力。
先做 provider 源码级验收，再接入 Nacos 和 RPC 调用验证。
```

阶段 3-4 仍然要守住边界：

```text
Dubbo 注解只能出现在 trigger 入站适配器。
domain 不能依赖 Dubbo。
api 只放对外契约和 DTO。
```

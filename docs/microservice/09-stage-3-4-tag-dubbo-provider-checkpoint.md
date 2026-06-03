# 阶段 3-4：tag-service Dubbo Provider 源码级实现与验收检查点

## 1. 本阶段目标

阶段 3-4 的目标是完成 `group-buy-tag-service` 的 Dubbo Provider 暴露能力。

本阶段只聚焦 provider 侧：

```text
让 tag-service 通过 Dubbo 暴露 ITagQueryService。
让 trigger 层 Provider 实现 api 契约，并调用 domain 层 ITagService。
让 app 层完成 Dubbo / Nacos 运行装配。
确认 Provider 可以注册到 Nacos。
```

本阶段不进入 activity / trade 消费方改造，不替换主链路，也不额外编写临时 RPC Test 消费者。

为什么这样划分：

```text
阶段 3 的目标是完成 tag-service 自身真实迁移和 provider 侧能力闭环。
RPC 消费验证应该放到阶段 4 的主链路改造中，由真实 activity / trade 消费方完成。
如果现在单独写临时 rpc test，只能证明一个测试消费者可以调通，后面仍然要再做真实消费者接入。
```

## 2. 本阶段完成了什么

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

当前完成范围：

```text
group-buy-tag-service/group-buy-tag-api
group-buy-tag-service/group-buy-tag-trigger
group-buy-tag-service/group-buy-tag-app
group-buy-tag-service/pom.xml
```

已完成内容：

- `ITagQueryService` 的返回值调整为 `Response<TagQueryResponseDTO>`。
- `group-buy-tag-trigger` 新增 `TagQueryProvider`。
- `TagQueryProvider` 使用 `@DubboService` 暴露 Dubbo Provider。
- `TagQueryProvider` 实现 `ITagQueryService`。
- `TagQueryProvider` 通过构造器注入 `ITagService`。
- Provider 对空请求、空 `userId`、空 `tagId` 返回 `Response.failure(ResponseCode.ILLEGAL_PARAMETER)`。
- Provider 对合法请求调用 `ITagService.matchCrowdTag(userId, tagId)`。
- Provider 正常返回 `Response.success(TagQueryResponseDTO)`。
- 父 POM 统一管理 Dubbo 相关版本。
- `group-buy-tag-trigger` 引入 `dubbo-spring-boot-starter`。
- `group-buy-tag-trigger` 显式引入 `group-buy-common-types`。
- `group-buy-tag-app` 引入 `dubbo-nacos-spring-boot-starter`。
- `TagApplication` 增加 `@EnableDubbo`。
- `application-dev.yml` 新增 Dubbo application、protocol、registry、scan 配置。
- 用户已在公司环境完成服务启动，并在 Nacos 看到 provider 注册。

Nacos 中已看到：

```text
cn.xuele.api.tag.ITagQueryService:::provider:group-buy-tag-service
cn.xuele.api.tag.ITagQueryService
```

## 3. API 契约验收

当前接口：

```text
group-buy-tag-api/src/main/java/cn/xuele/api/tag/ITagQueryService.java
```

当前契约：

```java
Response<TagQueryResponseDTO> matchCrowdTag(TagQueryRequestDTO request);
```

验收结论：

```text
ITagQueryService 已经具备 Dubbo RPC 契约形态。
请求 DTO 和响应 DTO 都实现 Serializable，并声明 serialVersionUID。
接口返回 Response<TagQueryResponseDTO> 后，可以区分调用成功、业务未命中、参数非法等语义。
```

为什么从 `TagQueryResponseDTO` 改为 `Response<TagQueryResponseDTO>`：

```text
如果只返回 TagQueryResponseDTO，参数非法、系统异常、业务未命中都容易混在一起。
matched=false 只能表达用户没有命中标签，不能表达调用方参数错误。
RPC 契约一旦被多个消费者依赖，后续再调整返回结构成本更高。
因此在服务拆分早期统一返回 Response<T>，更利于后续服务间调用的错误处理。
```

当前语义约定：

| 场景 | 返回 |
| --- | --- |
| 请求合法，用户命中标签 | `Response.success(data)`，`data.matched=true` |
| 请求合法，用户未命中标签 | `Response.success(data)`，`data.matched=false` |
| 请求为空或参数为空 | `Response.failure(ResponseCode.ILLEGAL_PARAMETER)` |
| Provider 内部未知异常 | 后续可扩展统一 RPC 异常处理，目前暂不展开 |

## 4. Provider 源码级验收

当前 Provider：

```text
group-buy-tag-trigger/src/main/java/cn/xuele/tag/trigger/rpc/TagQueryProvider.java
```

当前职责：

```text
接收 Dubbo RPC 请求。
完成请求参数校验。
调用 domain 层 ITagService。
把 domain 返回的 boolean 转换为 API 响应 DTO。
使用 Response<T> 表达调用结果。
```

验收结论：

```text
TagQueryProvider 是真实 Provider 实现，不是空实现或伪实现。
Provider 实现了 ITagQueryService。
Provider 只调用 ITagService.matchCrowdTag。
Provider 没有直接访问 Redis、MyBatis、DAO、Repository 实现类。
Provider 没有把 Dubbo 注解扩散到 domain、api、infrastructure。
```

关键实现语义：

```text
request == null -> ILLEGAL_PARAMETER
userId 或 tagId 为空白 -> ILLEGAL_PARAMETER
合法请求 -> tagService.matchCrowdTag(userId, tagId)
domain 返回 boolean -> TagQueryResponseDTO.matched
最终返回 Response.success(data)
```

为什么 Provider 不能直接访问 Redis / MyBatis：

```text
trigger 是入站适配器，只负责接收外部输入并调用领域服务。
Redis bitmap 和 MySQL 访问属于 infrastructure。
如果 Provider 直接访问 Redis 或 DAO，就绕过了 domain 层的业务入口，后续规则变更会散落在多个适配器中。
```

当前正确调用链：

```text
Dubbo Consumer
  -> ITagQueryService
  -> TagQueryProvider
  -> ITagService
  -> TagService
  -> ITagRepository
  -> TagRepository
  -> Redis / MySQL
```

## 5. Maven 依赖验收

### 5.1 父 POM

当前父 POM：

```text
group-buy-tag-service/pom.xml
```

已管理：

```xml
<dubbo.version>3.3.0</dubbo.version>
```

已纳入 `dependencyManagement`：

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>${dubbo.version}</version>
</dependency>

<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
    <version>${dubbo.version}</version>
</dependency>
```

验收结论：

```text
父 POM 只负责版本管理和依赖管理，没有把 Dubbo 强行加给所有子模块。
```

### 5.2 group-buy-tag-trigger

当前 trigger POM 已引入：

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>

<dependency>
    <groupId>cn.xuele</groupId>
    <artifactId>group-buy-common-types</artifactId>
</dependency>

<dependency>
    <groupId>cn.xuele</groupId>
    <artifactId>group-buy-tag-api</artifactId>
</dependency>

<dependency>
    <groupId>cn.xuele</groupId>
    <artifactId>group-buy-tag-domain</artifactId>
</dependency>
```

验收结论：

```text
trigger 直接使用 @DubboService，因此需要 dubbo-spring-boot-starter。
trigger 直接使用 Response 和 ResponseCode，因此显式依赖 common-types 是正确的。
trigger 实现 api 契约并调用 domain 服务，因此依赖 api 和 domain 是正确的。
```

### 5.3 group-buy-tag-app

当前 app POM 已引入：

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
</dependency>
```

验收结论：

```text
Nacos 注册发现属于服务运行装配能力，放在 app 层是合理的。
app 作为启动模块，负责把 trigger Provider 注册到 Nacos。
```

### 5.4 不应该引入 Dubbo 的模块

以下模块不应引入 Dubbo / Nacos：

```text
group-buy-tag-api
group-buy-tag-domain
group-buy-tag-infrastructure
group-buy-common-types
```

当前验收结论：

```text
api 没有 Dubbo 注解。
domain 没有 Dubbo、Nacos、MyBatis、Redis、Spring Web 依赖污染。
infrastructure 没有承接 Provider 注解。
common-types 没有框架依赖污染。
```

## 6. Dubbo / Nacos 配置验收

当前启动类：

```text
group-buy-tag-app/src/main/java/cn/xuele/tag/TagApplication.java
```

当前配置：

```java
@SpringBootApplication
@EnableDubbo
public class TagApplication {
    public static void main(String[] args) {
        SpringApplication.run(TagApplication.class);
    }
}
```

验收结论：

```text
@EnableDubbo 放在 app 启动层，符合运行装配边界。
```

当前 Dubbo 配置：

```text
group-buy-tag-app/src/main/resources/application-dev.yml
```

核心配置：

```yaml
dubbo:
  application:
    name: group-buy-tag-service
    logger: slf4j
  protocol:
    name: dubbo
    port: ${DUBBO_PROTOCOL_PORT:20891}
  registry:
    address: nacos://${NACOS_HOST:124.221.233.121}:${NACOS_PORT:8848}
  scan:
    base-packages: cn.xuele.tag.trigger.rpc
```

验收结论：

```text
Dubbo 应用名、协议端口、Nacos 注册中心和 Provider 扫描包已配置。
Dubbo 配置位于 app 运行配置中，没有进入 domain 或 api。
```

为什么 Provider 扫描包配置为 `cn.xuele.tag.trigger.rpc`：

```text
Dubbo Provider 是入站适配器，当前只允许 trigger/rpc 包承载 @DubboService。
明确扫描 trigger/rpc 可以避免后续其他包误放 Dubbo Provider。
```

## 7. 命令级验收说明

当前是公司环境，按协作规则：

```text
AI 不主动执行 Maven 编译、服务启动等构建命令。
编译和启动由用户在公司环境执行后反馈。
```

用户反馈：

```text
当前服务已经启动成功。
Nacos 中可以看到 ITagQueryService Provider。
```

命令级结论：

```text
tag-service Provider 注册链路已通过用户环境验证。
```

本阶段没有编写独立 RPC Test。

原因：

```text
当前阶段只要求 tag-service provider 侧完成源码闭环和注册验证。
真正 RPC 调用验证应在阶段 4 中由 activity / trade 等真实消费者完成。
临时 rpc test 消费者不是最终业务链路，价值低于真实消费者联调。
```

## 8. 本阶段修正过的问题

### 8.1 参数非法不能返回 matched=false

最初设计中，参数非法时曾考虑返回 `matched=false`。

最终修正为：

```text
参数非法 -> Response.failure(ResponseCode.ILLEGAL_PARAMETER)
请求合法但未命中 -> Response.success(data)，data.matched=false
```

为什么这样修：

```text
matched=false 是业务判断结果，不是参数校验结果。
如果把参数非法也返回 false，消费者无法区分调用错误和真实未命中。
```

### 8.2 参数空白判断

已将参数空白判断调整为 blank 语义。

原因：

```text
空字符串和纯空格字符串都不应该作为合法 userId / tagId。
```

### 8.3 trigger 显式依赖 common-types

Provider 直接使用 `Response` 和 `ResponseCode`，因此 `group-buy-tag-trigger` 显式依赖 `group-buy-common-types`。

原因：

```text
直接使用的类型应该由当前模块直接声明依赖。
不能长期依赖 api 的传递依赖。
```

### 8.4 引入 Dubbo Nacos starter

由于配置中使用：

```yaml
dubbo.registry.address: nacos://...
```

因此 app 层引入：

```text
org.apache.dubbo:dubbo-nacos-spring-boot-starter
```

原因：

```text
Nacos 注册中心能力是运行装配能力，由 app 层承载。
```

## 9. 阶段 3 总体验收结论

阶段 3 的目标是完成 tag-service 真实迁移。

阶段 3 已完成的子阶段：

| 子阶段 | 内容 | 状态 |
| --- | --- | --- |
| 3-1 | API / Domain 小步迁移 | 已完成 |
| 3-2 | Infrastructure / Repository 最小闭环 | 已完成 |
| 3-3 | App 装配与启动验收 | 已完成 |
| 3-4 | Dubbo Provider 暴露与 Nacos 注册 | 已完成 |

阶段 3 当前结论：

```text
group-buy-tag-service 的 provider 侧迁移已经正式收口。
tag-service 已具备独立启动、独立数据访问、Redis bitmap 查询、领域服务编排、Dubbo Provider 暴露和 Nacos 注册能力。
```

因此：

```text
阶段 3 可以正式完结。
```

但需要明确：

```text
阶段 3 完结不等于整条业务链路已经完成微服务化。
activity / trade 等服务尚未改为通过 Dubbo 调用 tag-service。
RPC 消费方调用验证后置到阶段 4 主链路改造中完成。
```

## 10. 当前遗留风险

### 10.1 RPC 消费方尚未接入

当前只完成 provider 注册。

风险：

```text
Nacos 能看到 Provider 只能证明服务注册成功。
还不能证明真实业务消费者可以完成 RPC 调用、超时处理、异常处理和 Response 解析。
```

处理计划：

```text
阶段 4 改造 activity / trade 主链路时，通过真实消费者调用 ITagQueryService 验证。
```

### 10.2 Response<T> 契约需要消费者统一处理

当前 API 返回 `Response<TagQueryResponseDTO>`。

风险：

```text
后续消费者必须先判断 response.isSuccess()，再读取 data。
如果消费者忽略 code，可能把失败响应当作正常未命中处理。
```

处理计划：

```text
阶段 4 设计消费者时统一约定 RPC 响应处理模板。
```

### 10.3 Provider 内部异常还没有统一兜底

当前 Provider 已处理参数非法，但没有单独设计全局 RPC 异常转换。

风险：

```text
如果 Redis、MySQL 或其他基础设施异常向上抛出，消费者可能收到 Dubbo 异常，而不是统一 Response 失败码。
```

处理计划：

```text
后续可在 trigger 层补充 RPC 异常处理策略。
当前阶段先保持最小闭环，不提前引入复杂治理。
```

### 10.4 DB 与 Redis 一致性风险仍存在

该风险来自阶段 3-2。

风险：

```text
crowd_tags_detail 写库和 Redis bitmap 写入不是同一事务。
极端情况下可能出现短暂不一致。
```

处理计划：

```text
后续通过补偿任务、幂等写入或事件驱动方式继续优化。
```

### 10.5 bitmap offset 仍存在低概率碰撞

该风险来自阶段 3-2。

风险：

```text
当前 userId offset 使用 hash + 取模，存在低概率碰撞。
```

处理计划：

```text
后续根据业务精度要求决定是否改为精确映射或分片 bitmap。
```

## 11. 为什么本阶段不写 RPC Test

当前不写临时 RPC Test 是可以接受的。

原因：

```text
阶段 3 的目标是 provider 侧真实迁移，不是完成跨服务主链路。
Provider 已完成源码级验收，并已在 Nacos 注册。
阶段 4 会引入真实消费者，届时 RPC 调用验证更贴近业务链路。
```

如果现在写临时 RPC Test：

```text
需要额外创建测试消费者、配置 Dubbo consumer、处理测试入口。
这个消费者后续不会成为真实业务代码。
```

更合适的验证时机：

```text
阶段 4：activity / trade 不再直接访问标签表或标签 bitmap。
阶段 4：真实消费者通过 ITagQueryService 调用 tag-service。
阶段 4：用真实业务接口验证 RPC 调用、Response 解析和异常语义。
```

## 12. 企业级体现

本阶段体现的企业级设计点：

- RPC Provider 只暴露稳定 API 契约，不暴露 domain / infrastructure 实现。
- 服务之间通过 `api` jar 依赖，不共享 DAO、PO、Repository。
- 参数非法和业务未命中使用不同语义表达。
- Dubbo 注解只出现在 trigger / app 边界内。
- Nacos 注册配置只放在 app 运行配置中。
- domain 层仍保持纯净，不依赖 Dubbo、Nacos、Spring Web、MyBatis、Redis。
- provider 侧先完成独立注册，再进入真实消费者改造，阶段边界清晰。

## 13. 面试表达

可以这样讲：

```text
我在 tag-service 拆分中没有让其他服务直接访问标签表或 Redis bitmap，而是把标签查询能力封装成 Dubbo RPC 契约。

tag-service 的 api 模块只暴露 ITagQueryService 和 DTO，trigger 层通过 @DubboService 实现 Provider，内部只调用 domain 层的 ITagService，不直接访问 Redis 或 MyBatis。

这样做可以保证调用入口统一经过领域服务，基础设施细节不会泄漏给其他服务。

在返回语义上，我把 RPC 返回统一成 Response<TagQueryResponseDTO>，这样消费者可以区分调用成功、参数非法和业务未命中，而不是把参数错误也误判为 matched=false。

Provider 注册能力由 app 层通过 Dubbo 和 Nacos 配置完成，domain 层仍然不依赖 Dubbo、Nacos、Redis、MyBatis 等外部技术，符合六边形架构的依赖方向。
```

如果面试官问：“为什么不在阶段 3 就写 RPC Test？”

可以回答：

```text
阶段 3 我主要验证 tag-service provider 侧迁移是否完整，包括源码实现、依赖边界、启动装配和 Nacos 注册。

真正有价值的 RPC 调用验证应该放到阶段 4，因为那时 activity 或 trade 会作为真实消费者调用 tag-service。

如果阶段 3 单独写一个临时 consumer，只能证明测试消费者能调通，后续仍然要再验证真实业务链路。

所以我把 provider 注册作为阶段 3 验收点，把真实 RPC 调用和异常处理放到阶段 4 主链路改造中验证。
```

## 14. 下一阶段计划

阶段 3 已正式收口。

下一阶段进入阶段 4：主链路改造。

阶段 4 的核心目标：

```text
activity / trade 不再直接访问标签表。
activity / trade 不再直接访问标签 Redis bitmap。
真实消费者通过 ITagQueryService 调用 tag-service。
验证 Response<TagQueryResponseDTO> 的成功、失败、未命中语义。
完成真实 RPC 调用链路验收。
```

阶段 4 开始前建议先做：

- 梳理旧单体中哪些代码直接访问标签表。
- 梳理旧单体中哪些代码直接访问标签 bitmap。
- 确认哪些业务链路需要判断用户是否命中标签。
- 设计 activity / trade 侧作为 Dubbo Consumer 的依赖和配置边界。
- 明确消费者如何处理 `Response<TagQueryResponseDTO>`。


# 阶段 3：tag-service 人群标签服务检查点

## 1. 阶段目标

阶段 3 的目标是完成 `tag-service` provider 侧真实迁移。

这个阶段要把旧单体中的人群标签能力拆成独立微服务，并守住几个核心边界：

```text
tag-service 拥有人群标签表、标签任务表、标签明细表和标签 Redis bitmap。
其他服务不能直接访问 crowd_tags_detail 或 crowd:tag:bitmap:*。
其他服务只能通过 tag-service API / Dubbo 查询用户是否命中标签。
tag-domain 保持纯 Java，不依赖 MyBatis、Redis、Dubbo、Spring Web、Nacos 或运行配置。
Redis bitmap key、offset 算法、MyBatis DAO、PO 和 Mapper XML 都属于 infrastructure。
Dubbo Provider 属于 trigger 入站适配器。
Spring Boot 启动、Bean 装配、Dubbo / Nacos / MyBatis / Redis 配置属于 app。
```

阶段 3 当前结论：

```text
group-buy-tag-service 已完成 provider 侧迁移。
服务可以独立启动，注册到 Nacos，并通过 Dubbo 暴露 ITagQueryService。
阶段 4 中 activity-service 已作为真实消费者调用 tag-service，并完成真实链路验证。
```

## 2. 业务场景

tag-service 本质上是人群标签服务，负责把一批用户圈入某个人群标签，并支持后续快速判断某个用户是否命中该标签。

当前业务分两条线。

### 2.1 标签生产线

入口语义：

```text
executeCrowdTagBatch(tagId, batchId)
```

业务流程：

```text
根据 tagId + batchId 查询 crowd_tags_job
  -> 获取标签任务规则，例如 tagType、tagRule、统计时间范围
  -> 计算或圈选用户列表
  -> 写入 crowd_tags_detail
  -> 更新 crowd_tags.statistics
  -> 写入 Redis bitmap：crowd:tag:bitmap:{tagId}
```

当前用户圈选仍是学习阶段的模拟实现：

```java
List<String> userIdList = List.of("xuele", "keke", "bangzhi");
```

这说明生产线的领域编排和基础设施闭环已经具备，但真实标签规则计算、规则解析和用户圈选还没有展开。

### 2.2 标签查询线

入口语义：

```text
matchCrowdTag(userId, tagId)
```

业务流程：

```text
根据 tagId 生成 Redis bitmap key
  -> 根据 userId 计算 bitmap offset
  -> 从 Redis bitmap 读取 offset 是否为 true
  -> 对外返回 matched
```

这是 activity-service 当前真实使用的链路。

## 3. 完成范围

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

已完成模块：

```text
group-buy-tag-service/group-buy-tag-api
group-buy-tag-service/group-buy-tag-domain
group-buy-tag-service/group-buy-tag-infrastructure
group-buy-tag-service/group-buy-tag-trigger
group-buy-tag-service/group-buy-tag-app
```

已完成能力：

- `group-buy-tag-api` 完成 Dubbo RPC 契约和请求 / 响应 DTO。
- `group-buy-tag-domain` 完成 `ITagService`、`TagService`、`ITagRepository` 和 `CrowdTagsJobEntity`。
- `group-buy-tag-infrastructure` 完成 MyBatis DAO、PO、Mapper XML、Redis 配置、bitmap key / offset 工具和 `TagRepository`。
- `group-buy-tag-trigger` 完成 Dubbo Provider `TagQueryProvider`。
- `group-buy-tag-app` 完成启动类、领域服务装配、MyBatis / Redis / Dubbo / Nacos / 日志配置。
- 用户已在公司环境完成服务启动，并在 Nacos 看到 tag-service provider。
- 阶段 4 已通过 activity-service 真实消费者验证 tag-service 查询能力。

## 4. 当前源码结构

API：

```text
group-buy-tag-api
  cn.xuele.tag.api.ITagQueryService
  cn.xuele.tag.api.dto.TagQueryRequestDTO
  cn.xuele.tag.api.dto.TagQueryResponseDTO
```

Domain：

```text
group-buy-tag-domain
  cn.xuele.tag.domain.service.ITagService
  cn.xuele.tag.domain.service.TagService
  cn.xuele.tag.domain.adapter.ITagRepository
  cn.xuele.tag.domain.model.entity.CrowdTagsJobEntity
```

Infrastructure：

```text
group-buy-tag-infrastructure
  cn.xuele.tag.infrastructure.adapter.repository.TagRepository
  cn.xuele.tag.infrastructure.dao.ICrowdTagsDao
  cn.xuele.tag.infrastructure.dao.ICrowdTagsDetailDao
  cn.xuele.tag.infrastructure.dao.ICrowdTagsJobDao
  cn.xuele.tag.infrastructure.dao.po.CrowdTags
  cn.xuele.tag.infrastructure.dao.po.CrowdTagsDetail
  cn.xuele.tag.infrastructure.dao.po.CrowdTagsJob
  cn.xuele.tag.infrastructure.redis.RedisClientConfig
  cn.xuele.tag.infrastructure.redis.RedisClientConfigProperties
  cn.xuele.tag.infrastructure.redis.TagBitmapUtils
  mybatis/mapper/crowd_tags_mapper.xml
  mybatis/mapper/crowd_tags_detail_mapper.xml
  mybatis/mapper/crowd_tags_job_mapper.xml
```

Trigger：

```text
group-buy-tag-trigger
  cn.xuele.tag.trigger.rpc.TagQueryProvider
```

App：

```text
group-buy-tag-app
  cn.xuele.tag.TagApplication
  cn.xuele.tag.app.config.DomainServiceConfig
  application-dev.yml
  logback-spring.xml
```

## 5. 核心调用链路

标签查询 RPC 链路：

```text
Dubbo Consumer
  -> ITagQueryService.matchCrowdTag(TagQueryRequestDTO)
  -> TagQueryProvider
  -> ITagService.matchCrowdTag(userId, tagId)
  -> TagService
  -> ITagRepository.isUserMatchedTag(userId, tagId)
  -> TagRepository
  -> Redis bitmap
  -> Response<TagQueryResponseDTO>
```

标签生产线链路：

```text
ITagService.executeCrowdTagBatch(tagId, batchId)
  -> TagService
  -> ITagRepository.queryCrowdTagsJob(tagId, batchId)
  -> ITagRepository.saveCrowdTagUsers(tagId, userIdList)
  -> ITagRepository.updateCrowdTagStatistics(tagId, count)
  -> MySQL crowd_tags_detail / crowd_tags
  -> Redis bitmap
```

当前标签生产线还没有 trigger 入口，属于后续优化项。

## 6. API 契约

当前 RPC 契约：

```java
Response<TagQueryResponseDTO> matchCrowdTag(TagQueryRequestDTO request);
```

请求 DTO：

```text
userId：用户ID
tagId：标签ID
```

响应 DTO：

```text
userId：用户ID
tagId：标签ID
matched：是否命中标签
```

语义约定：

| 场景 | 返回 |
| --- | --- |
| 请求合法，用户命中标签 | `Response.success(data)`，`data.matched=true` |
| 请求合法，用户未命中标签 | `Response.success(data)`，`data.matched=false` |
| 请求为空或参数为空白 | `Response.failure(ResponseCode.ILLEGAL_PARAMETER)` |
| Provider 内部未知异常 | 当前会向上抛出，后续可补统一 RPC 异常转换 |

为什么使用 `Response<TagQueryResponseDTO>`：

```text
matched=false 只能表达用户没有命中标签。
参数非法、调用失败、系统异常不能和业务未命中混在一起。
RPC 契约一旦被 activity / trade 等服务依赖，后续再调整返回结构成本更高。
```

## 7. Repository 与 Redis bitmap

`TagRepository` 实现了领域端口 `ITagRepository`。

### 7.1 isUserMatchedTag

实现语义：

```text
根据 tagId 生成 Redis bitmap key。
根据 userId 计算 bitmap offset。
从 Redis bitmap 读取该 offset 是否为 true。
```

当前 Redis key 格式：

```text
crowd:tag:bitmap:{tagId}
```

bitmap offset 算法：

```text
MurmurHash3_32(userId)
  -> 正整数
  -> 对 100000000 取模
```

这个方案空间友好、查询快，但存在低概率 hash 碰撞。后续如果业务不允许误命中，需要结合明细表二次校验或更精确的数据结构。

### 7.2 queryCrowdTagsJob

实现语义：

```text
构建 CrowdTagsJob 查询 PO。
通过 ICrowdTagsJobDao 查询 crowd_tags_job。
将 PO 转换为 CrowdTagsJobEntity 返回给 domain。
```

### 7.3 saveCrowdTagUsers

实现语义：

```text
过滤无效参数。
将 userIdList 转换为 CrowdTagsDetail PO 列表。
批量写入 crowd_tags_detail。
批量写入 Redis bitmap。
```

这里是阶段 3 的关键闭环，但也带来一致性风险：

```text
MySQL 与 Redis bitmap 不是同一事务。
MySQL 写入成功但 Redis 写入失败时，查询线可能暂时读不到最新标签。
后续需要补偿任务、bitmap 重建机制或可靠事件驱动来兜底。
```

### 7.4 updateCrowdTagStatistics

实现语义：

```text
构建 CrowdTags 更新 PO。
根据 tagId 更新 crowd_tags.statistics。
```

## 8. App 装配与运行配置

当前启动类：

```text
cn.xuele.tag.TagApplication
```

关键配置：

```java
@SpringBootApplication(scanBasePackages = "cn.xuele.tag")
@EnableDubbo
public class TagApplication {
    public static void main(String[] args) {
        SpringApplication.run(TagApplication.class);
    }
}
```

启动类放在 `cn.xuele.tag` 根包下，并显式配置 `scanBasePackages = "cn.xuele.tag"`，可以覆盖 app、trigger、infrastructure、domain 相关包。

领域服务装配：

```java
@Bean
public ITagService tagService(ITagRepository tagRepository) {
    return new TagService(tagRepository);
}
```

为什么不在 `TagService` 上加 `@Service`：

```text
domain 层表达业务规则，不应该依赖 Spring。
app 层是组合根，负责把领域服务和基础设施适配器装配成 Spring Bean。
```

当前运行配置：

```text
server.port = 8091
dubbo.protocol.port = 20891
dubbo.scan.base-packages = cn.xuele.tag.trigger.rpc
datasource schema = group_buy_tag
redis.sdk.config.mode = standalone
```

当前 dev 配置默认 Redis：

```text
host = 124.221.233.121
port = 16379
password = xuele_redis_123456
```

旧工作区还保留了本地 Docker Redis compose：

```text
docs/microservice/redis/docker-compose.yml
container_name = xuele-redis
ports = 16379:6379
requirepass = xuele_redis_123456
```

如果应用在宿主机访问本地 Docker Redis，可以通过环境变量覆盖：

```text
REDIS_HOST=127.0.0.1
REDIS_PORT=16379
REDIS_PASSWORD=xuele_redis_123456
```

如果应用也放进同一个 Docker 网络，则应改为访问容器名和容器端口：

```text
REDIS_HOST=xuele-redis
REDIS_PORT=6379
```

## 9. Dubbo / Nacos Provider

当前 Provider：

```text
cn.xuele.tag.trigger.rpc.TagQueryProvider
```

Provider 职责：

```text
接收 Dubbo RPC 请求。
完成请求参数校验。
调用 domain 层 ITagService。
把 domain 返回的 boolean 转换为 API 响应 DTO。
使用 Response<T> 表达调用结果。
```

Provider 不做的事情：

```text
不直接访问 Redis。
不直接访问 MyBatis DAO。
不直接访问 Repository 实现类。
不承载标签规则计算。
不把 Dubbo 注解扩散到 domain、api、infrastructure。
```

当前 Dubbo 配置：

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

Provider 扫描包固定到 `cn.xuele.tag.trigger.rpc`，明确只有 trigger/rpc 承载 Dubbo Provider。

## 10. 数据归属

tag-service 自有 schema：

```text
group_buy_tag
```

自有表：

```text
crowd_tags
crowd_tags_detail
crowd_tags_job
```

Redis key 归属：

```text
crowd:tag:bitmap:{tagId}
```

服务边界结论：

```text
activity-service / trade-service 不能直接访问 crowd_tags_detail。
activity-service / trade-service 不能直接访问 crowd:tag:bitmap:{tagId}。
activity-service / trade-service 只能通过 cn.xuele.tag.api.ITagQueryService 查询标签命中。
```

当前初始化 SQL 位置：

```text
旧工作区：
docs/microservice/sql/01-init-group-buy-tag.sql

新微服务工作区：
mysql-microservice/tag/01-init-group-buy-tag.sql
```

注意：SQL 明细表存在用户记录，不代表 Redis bitmap 已经初始化。当前查询线读取 Redis bitmap，因此 Redis 未置位时会返回 `matched=false`。

## 11. 阶段 4 回填验证

阶段 3 结束时，tag-service 只完成 provider 侧迁移和 Nacos 注册。

阶段 4 中，activity-service 已作为真实消费者完成验证：

```text
activity-service
  -> TagQueryPort
  -> @DubboReference ITagQueryService
  -> tag-service TagQueryProvider
  -> TagService
  -> TagRepository
  -> Redis bitmap
```

验证现象：

```text
Redis bitmap 未初始化时：
activity 返回 visible=false、enable=false、deductionPrice=0.00、payPrice=100.00。

Redis bitmap 补充 xuele 命中位后：
activity 返回 visible=true、enable=true、deductionPrice=10.00、payPrice=90.00。
```

这说明：

```text
tag-service Provider 可以被真实业务消费者调用。
Response<TagQueryResponseDTO> 的 matched 语义被 activity-service 正确消费。
标签服务自治 Redis bitmap，activity-service 不直接访问标签表或 Redis key。
```

## 12. 源码级验收

已完成检查：

```text
group-buy-tag-api 只暴露 RPC 契约和 DTO，不依赖 domain / infrastructure。
group-buy-tag-domain 没有 Spring、MyBatis、Redis、Redisson、Dubbo、Nacos、Spring Web 依赖污染。
TagService 是纯 Java 领域服务，由 app 层装配。
TagRepository 是 infrastructure 对 ITagRepository 的真实实现，不是空实现。
Redis key 和 offset 算法只在 infrastructure。
MyBatis DAO、PO、Mapper XML 均留在 infrastructure。
TagQueryProvider 只做入站适配，不直接访问 Redis / DAO。
Dubbo Provider 注解只出现在 trigger。
Dubbo / Nacos 配置只出现在 app 运行配置。
```

公司环境说明：

```text
阶段 3 的编译、启动和 Nacos 注册由用户在公司环境完成。
AI 不主动在公司环境执行 Maven 编译和服务启动。
```

## 13. 当前遗留风险

### 13.1 DB 与 Redis bitmap 一致性

当前标签生产线是先写 MySQL 明细，再写 Redis bitmap。

风险：

```text
MySQL 和 Redis 不属于同一事务。
Redis 写入失败或部分成功会导致查询线短暂不一致。
```

后续建议：

```text
补偿任务。
bitmap 重建机制。
可靠事件驱动 Redis bitmap 更新。
```

### 13.2 重复执行批次的幂等问题

`crowd_tags_detail` 有唯一索引：

```text
UNIQUE KEY uq_tag_user(tag_id, user_id)
```

当前批量 insert 遇到重复数据可能抛唯一索引冲突。

后续要根据业务选择：

```text
insert ignore
on duplicate key update
批次幂等状态机
```

### 13.3 bitmap hash 碰撞

当前 offset 算法使用 MurmurHash3 + 取模，存在低概率碰撞。

后续要根据业务精度决定：

```text
是否接受低概率误命中。
是否结合明细表二次确认。
是否改为更精确的数据结构或分片方案。
```

### 13.4 标签规则生产仍是模拟

当前 `executeCrowdTagBatch` 中用户列表仍然是模拟数据。

后续需要补：

```text
标签规则来源。
规则解析。
用户圈选逻辑。
任务状态流转。
任务幂等和重试。
```

### 13.5 Provider 内部异常还没有统一转换

当前 Provider 已处理参数非法，但没有统一兜底基础设施异常。

后续可在 trigger 层补：

```text
RPC 入口日志。
AppException / 系统异常转换。
统一失败响应。
调用耗时监控。
```

## 14. 面试表达

可以这样表达阶段 3：

```text
在拆 tag-service 时，我没有让其他服务直接访问标签表或 Redis bitmap，而是把标签能力封装成独立的 Dubbo RPC 契约。

tag-service 内部按六边形架构拆分：api 只放 ITagQueryService 和 DTO；domain 只表达 ITagService、TagService 和 ITagRepository；infrastructure 负责 MyBatis、Redis bitmap 和 Repository 实现；trigger 通过 @DubboService 暴露 Provider；app 负责 Spring Boot 启动、Bean 装配和 Dubbo / Nacos 配置。

标签查询返回 Response<TagQueryResponseDTO>，这样消费者可以区分参数非法、调用失败和用户真实未命中，而不是把所有情况都混成 matched=false。

Redis bitmap 的 key 和 offset 算法归 tag-service infrastructure 所有，activity-service 不知道也不能直接访问。阶段 4 里 activity-service 通过 ITagQueryService 调用 tag-service，完成了真实消费者验证。

我也明确记录了标签生产线的 DB 与 Redis 非事务一致性风险，后续会通过补偿任务或 bitmap 重建机制兜底。
```

如果面试官问：“为什么阶段 3 不单独写临时 RPC Test？”

可以回答：

```text
阶段 3 的目标是 provider 侧迁移和注册闭环。真正有价值的 RPC 验证应该来自真实业务消费者。

如果阶段 3 写临时 consumer，只能证明测试消费者可以调通，后续 activity 或 trade 接入时仍然要再验证一遍。

所以我把 provider 注册作为阶段 3 验收点，把真实消费者验证放到阶段 4。阶段 4 中 activity-service 已经通过 Dubbo 调用 tag-service，并完成 HTTP 到 activity 再到 tag-service 和 Redis bitmap 的完整链路验证。
```


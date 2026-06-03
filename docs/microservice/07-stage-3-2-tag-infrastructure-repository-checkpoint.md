# 阶段 3-2：tag-service Infrastructure / Repository 最小闭环检查点

## 1. 本阶段目标

阶段 3-2 的目标是完成 `group-buy-tag-service` 的基础设施层最小闭环。

本阶段不进入 Dubbo、Nacos、HTTP Controller、Job 调度和服务启动验收，只聚焦：

```text
把 tag 业务需要的 MySQL / Redis 访问能力迁移到 infrastructure。
让 domain 通过 ITagRepository 端口调用基础设施能力。
验证 Repository 不是空实现、伪实现，而是真实完成 MySQL 与 Redis bitmap 的读写。
```

本阶段完成后，tag-service 已具备标签任务查询、标签用户明细写入、标签统计更新、标签 bitmap 命中查询的基础设施能力。

## 2. 本阶段完成了什么

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

当前完成范围：

```text
group-buy-tag-service/group-buy-tag-infrastructure
group-buy-tag-service/group-buy-tag-app/src/main/resources/application-dev.yml
```

已完成内容：

- 父 POM 统一管理 MyBatis、MySQL、Redisson、Guava 版本。
- `group-buy-tag-infrastructure` 引入 MyBatis、MySQL、Redisson、Guava、Lombok。
- 迁移 `CrowdTags`、`CrowdTagsDetail`、`CrowdTagsJob` 三个 PO。
- 迁移 `ICrowdTagsDao`、`ICrowdTagsDetailDao`、`ICrowdTagsJobDao` 三个 DAO。
- 迁移 `crowd_tags_mapper.xml`、`crowd_tags_detail_mapper.xml`、`crowd_tags_job_mapper.xml`。
- 新增 `RedisClientConfigProperties`，用于绑定 `redis.sdk.config` 配置。
- 新增 `RedisClientConfig`，支持 Redis standalone / cluster 两种部署模式。
- 新增 `TagBitmapUtils`，封装 tag-service 自己的 bitmap key 和 userId offset 算法。
- 完成 `TagRepository` 对 `ITagRepository` 的真实实现。

## 3. Repository 源码级验收

当前 `TagRepository` 实现了领域端口 `ITagRepository` 的四个方法。

### 3.1 isUserMatchedTag

实现语义：

```text
根据 tagId 生成 Redis bitmap key。
根据 userId 计算 bitmap offset。
从 Redis bitmap 中读取该 offset 是否为 true。
```

验收结论：

```text
该方法是真实 Redis bitmap 查询实现，不是空实现。
```

注意：

```text
offset 使用 MurmurHash + 取模计算，是空间友好的快速判断方案，但不是绝对无碰撞的精确映射。
```

### 3.2 queryCrowdTagsJob

实现语义：

```text
构建 CrowdTagsJob 查询 PO。
通过 ICrowdTagsJobDao 查询 crowd_tags_job。
将 PO 转换为 CrowdTagsJobEntity 返回给 domain。
```

验收结论：

```text
该方法完成了 MyBatis 查询和 PO -> Domain Entity 转换。
```

注意：

```text
当前只查询领域层需要的 tagType、tagRule、statStartTime、statEndTime。
```

### 3.3 saveCrowdTagUsers

实现语义：

```text
过滤无效参数。
将 userIdList 转换为 CrowdTagsDetail PO 列表。
批量写入 crowd_tags_detail。
批量写入 Redis bitmap。
```

验收结论：

```text
该方法完成了标签用户明细落库和 Redis bitmap 写入，是本阶段最核心的基础设施闭环。
```

注意：

```text
当前 DB 与 Redis bitmap 非同一事务。
Redis 写入失败或部分成功时，可能产生短暂不一致。
后续需要通过补偿任务或 bitmap 重建机制兜底。
```

### 3.4 updateCrowdTagStatistics

实现语义：

```text
构建 CrowdTags 更新 PO。
根据 tagId 更新 crowd_tags.statistics。
```

验收结论：

```text
该方法完成了标签主表统计量更新。
```

## 4. DAO / PO / Mapper XML 验收

本阶段确认了 DAO、PO、Mapper XML 的映射关系。

### 4.1 crowd_tags

文件：

```text
ICrowdTagsDao
CrowdTags
crowd_tags_mapper.xml
```

已对齐内容：

```text
namespace = cn.xuele.tag.infrastructure.dao.ICrowdTagsDao
方法 id = updateCrowdTagStatistics
parameterType = cn.xuele.tag.infrastructure.dao.po.CrowdTags
```

### 4.2 crowd_tags_detail

文件：

```text
ICrowdTagsDetailDao
CrowdTagsDetail
crowd_tags_detail_mapper.xml
```

已对齐内容：

```text
namespace = cn.xuele.tag.infrastructure.dao.ICrowdTagsDetailDao
方法 id = addCrowdTagsUsers
parameterType = java.util.List
foreach collection = list
```

本阶段已删除：

```text
queryTagIdByUserId
```

删除原因：

```text
一个用户可能命中多个标签，按 userId 查询单个 tagId 语义不稳定。
该方法也不是当前 tag-service Repository 闭环必需能力。
```

### 4.3 crowd_tags_job

文件：

```text
ICrowdTagsJobDao
CrowdTagsJob
crowd_tags_job_mapper.xml
```

已对齐内容：

```text
namespace = cn.xuele.tag.infrastructure.dao.ICrowdTagsJobDao
方法 id = queryCrowdTagsJobEntity
parameterType = cn.xuele.tag.infrastructure.dao.po.CrowdTagsJob
resultMap = dataMap
```

## 5. Redis 配置验收

当前 Redis 配置类为：

```text
RedisClientConfigProperties
RedisClientConfig
TagBitmapUtils
```

### 5.1 配置绑定

`RedisClientConfigProperties` 绑定前缀：

```text
redis.sdk.config
```

`application-dev.yml` 当前配置：

```text
redis:
  sdk:
    config:
      mode: standalone
      standalone:
        host: ${REDIS_HOST:124.211.233.121}
        port: ${REDIS_PORT:16379}
```

验收结论：

```text
配置前缀与属性类绑定一致。
```

### 5.2 standalone / cluster 支持

当前 `RedisClientConfig` 支持：

```text
mode = standalone
mode = cluster
```

standalone 模式：

```text
使用 standalone.host + standalone.port 构造 redis://host:port。
```

cluster 模式：

```text
读取 cluster.nodes。
过滤 null 和空白节点。
trim 节点字符串。
为未带 redis:// 前缀的节点补齐协议。
过滤后再次校验节点不能为空。
```

验收结论：

```text
Redis 部署模式由配置决定，Repository 不感知 standalone 或 cluster。
这符合基础设施配置与业务仓储实现解耦的要求。
```

### 5.3 bitmap key 和 offset

`TagBitmapUtils` 当前负责：

```text
生成 Redis key：crowd:tag:bitmap:{tagId}
将 userId 映射为 bitmap offset
```

验收结论：

```text
Redis key 和 offset 算法留在 infrastructure，没有进入 domain。
```

## 6. 依赖边界验收

当前 Maven 依赖方向仍符合阶段 2 定下的边界：

```text
api -> common-types
domain -> common-types
infrastructure -> domain
trigger -> api + domain
app -> trigger + infrastructure
```

基础设施依赖只进入 `group-buy-tag-infrastructure`：

```text
org.mybatis.spring.boot:mybatis-spring-boot-starter
com.mysql:mysql-connector-j
org.redisson:redisson-spring-boot-starter
com.google.guava:guava
```

未污染：

```text
group-buy-common-types
group-buy-tag-api
group-buy-tag-domain
```

验收结论：

```text
domain 仍不依赖 MyBatis、Redis、Redisson、Spring Web、Dubbo、Nacos。
api 仍不依赖 domain、infrastructure、trigger。
```

## 7. 为什么这样设计

本阶段的关键设计是让 `TagRepository` 成为领域端口的基础设施适配器。

领域层只知道：

```text
我要查询标签任务。
我要保存标签用户。
我要更新标签统计。
我要判断用户是否命中标签。
```

领域层不知道：

```text
MyBatis Mapper 怎么写。
Redis bitmap key 怎么拼。
Redisson 是单机还是集群。
MySQL 表字段如何映射。
```

这样做的收益是：

- domain 保持业务语义纯净。
- Redis / MySQL 细节集中在 infrastructure。
- 后续如果 Redis 部署方式从单机切换为集群，不需要改 Repository 的业务调用语义。
- 后续 Activity / Trade 服务只能通过 tag-service API 查询标签能力，不能直接访问标签表或标签 bitmap。

## 8. 企业级体现在哪里

本阶段体现的企业级标准：

- 不是只看 Maven 编译，而是逐项源码级验收 Repository 方法。
- DAO / PO / Mapper XML 按表归属和方法语义逐一对齐。
- Redis key 归 tag-service 所有，避免多个服务随意读写同一类缓存。
- Redis standalone / cluster 通过配置切换，业务仓储不感知部署模式。
- 发现 DB / Redis 一致性风险后明确记录，而不是用事务注解误认为已经解决。
- 删除语义不稳定且当前未使用的 `queryTagIdByUserId`。
- 使用 tag 专属数据库账号 `tag_user`，密码与初始化 SQL 保持一致。

## 9. 当前还遗留什么风险

本阶段源码级闭环已经完成，但仍有以下遗留风险：

### 9.1 DB 与 Redis bitmap 一致性

当前 `saveCrowdTagUsers` 是：

```text
先写 MySQL crowd_tags_detail。
再写 Redis bitmap。
```

风险：

```text
MySQL 与 Redis 不属于同一事务。
Redis 写入失败或部分成功时，可能出现短暂不一致。
```

后续方案：

```text
引入补偿任务。
支持按 crowd_tags_detail 重建 bitmap。
或者通过可靠事件驱动 Redis bitmap 更新。
```

### 9.2 重复执行批次导致重复插入

`crowd_tags_detail` 存在唯一索引：

```text
UNIQUE KEY uq_tag_user(tag_id, user_id)
```

当前 XML 使用普通批量 insert。

风险：

```text
同一个 tagId + userId 重复插入时，会触发 DuplicateKeyException。
```

后续方案：

```text
根据业务选择 insert ignore、on duplicate key update，或在领域层保证批次幂等。
```

### 9.3 bitmap hash 碰撞

当前 offset 算法：

```text
MurmurHash3_32(userId) -> 正整数 -> 对 100000000 取模
```

风险：

```text
该方案节省空间，但存在低概率 hash 碰撞。
```

后续方案：

```text
评估业务是否接受误命中。
如不接受，需要结合明细表二次校验或更精确的数据结构。
```

### 9.4 标签规则计算仍是临时模拟

当前 domain 的 `executeCrowdTagBatch` 中，用户列表仍是临时模拟数据。

风险：

```text
阶段 3-2 只证明基础设施闭环可用，不代表真实标签规则计算已经完成。
```

后续方案：

```text
后续接入真实规则来源、规则解析、用户圈选逻辑。
```

## 10. 面试表达

可以这样表达本阶段：

```text
在 tag-service 的基础设施迁移中，我没有把旧单体代码直接复制进来，而是先按照六边形架构把领域端口和基础设施实现分开。

domain 层只定义 ITagRepository，表达标签任务查询、标签用户保存、标签统计更新、用户命中判断这些业务需要的数据能力。

infrastructure 层实现这个端口，内部通过 MyBatis 访问 crowd_tags、crowd_tags_detail、crowd_tags_job 三张归 tag-service 所有的表，并通过 Redisson 读写 Redis bitmap。

Redis bitmap 的 key 和 offset 算法也只放在 infrastructure，domain 不感知 Redis。Redis 单机和集群通过配置切换，Repository 不关心具体部署模式。

同时我也记录了 MySQL 与 Redis 非同一事务带来的一致性风险，后续会通过补偿任务或 bitmap 重建机制兜底。
```

## 11. 阶段 3-2 验收结论

源码级结论：

```text
阶段 3-2：tag-service infrastructure / Repository 最小闭环完成。
```

边界结论：

```text
基础设施依赖没有污染 api、domain、common-types。
Redis / MyBatis / MySQL 细节均留在 infrastructure。
```

命令级验收：

```text
当前是公司环境，AI 不主动执行 Maven 编译。
编译由用户在公司环境自行执行。
```

建议用户执行：

```text
cd D:\Code4J\group-buy-microservice\group-buy-tag-service
mvn clean compile
```

## 12. 下一阶段应该做什么

阶段 3-2 完成后，下一步进入阶段 3-3。

阶段 3-3 目标：

```text
完成 tag-service app 装配与启动验收。
检查 Spring Boot 启动类。
检查模块扫描范围。
检查 MyBatis Mapper XML 扫描。
检查 infrastructure Bean 是否能被 app 装配。
在通过源码级检查后，再由用户在公司环境执行编译和启动验证。
```

阶段 3-3 暂不急着做 Dubbo / Nacos。

建议顺序：

```text
先让 tag-service app 能正确装配 domain + infrastructure。
再进入 Dubbo Provider 暴露标签查询契约。
最后再接入 Nacos 注册发现。
```

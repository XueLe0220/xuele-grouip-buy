# 阶段 3-1：tag-service API / Domain 迁移检查点

## 1. 本阶段目标

阶段 3 正式进入 `group-buy-tag-service` 真实业务迁移。

本检查点先完成第一小步：

```text
先迁移 tag-service 的 api 契约和 domain 领域骨架。
暂不迁移 MyBatis、Redis、Dubbo Provider、Spring Boot 配置。
```

这样做的原因是：

- 先稳定服务对外契约，后面 Activity / Trade 只能依赖 `group-buy-tag-api`。
- 先确定 domain 的业务语言和仓储端口，避免直接复制旧代码时把 MyBatis、Redis、Dubbo、Spring 注解带进领域层。
- 先按业务能力迁移，而不是按旧项目包名机械复制。

## 2. 本阶段梳理的原业务

旧单体里的 tag 业务本质是“人群标签服务”。

它负责把符合规则的一批用户圈入某个人群标签，并支持后续快速判断某个用户是否命中该标签。

当前业务可以拆成两条链路：

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
-> 写入 Redis bitmap: crowd:tag:bitmap:{tagId}
```

旧代码中圈选用户列表目前还是模拟数据，后续迁移时可以先保留业务骨架，再逐步接入真实规则来源。

### 2.2 标签查询线

入口语义：

```text
matchCrowdTag(userId, tagId)
```

业务流程：

```text
根据 userId + tagId 判断用户是否命中人群标签
-> tag-service 内部通过 Redis bitmap 查询
-> 对外通过 api 契约返回 matched
```

后续 Activity / Trade 不应该直接访问 `crowd_tags_detail` 或 Redis bitmap，而应该通过 tag-service 暴露的契约调用标签能力。

## 3. 旧项目参考位置

旧单体项目路径：

```text
D:\Code4J\xuele-group-buy
```

重点参考文件：

```text
xuele-group-buy-domain/src/main/java/cn/xuele/domain/tag/service/TagService.java
xuele-group-buy-domain/src/main/java/cn/xuele/domain/tag/service/ITagService.java
xuele-group-buy-domain/src/main/java/cn/xuele/domain/tag/adapter/repository/ITagRepository.java
xuele-group-buy-domain/src/main/java/cn/xuele/domain/tag/model/entity/CrowdTagsJobEntity.java

xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/adapter/repository/TagRepository.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/ICrowdTagsDao.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/ICrowdTagsDetailDao.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/ICrowdTagsJobDao.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/po/CrowdTags.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/po/CrowdTagsDetail.java
xuele-group-buy-infrastructure/src/main/java/cn/xuele/infrastructure/dao/po/CrowdTagsJob.java
xuele-group-buy-infrastructure/src/main/resources/mybatis/mapper/crowd_tags_mapper.xml
xuele-group-buy-infrastructure/src/main/resources/mybatis/mapper/crowd_tags_job_mapper.xml
xuele-group-buy-infrastructure/src/main/resources/mybatis/mapper/IRefundOrderStrategy.xml
```

注意：

```text
旧工作区里早期手写的小 demo xuele-group-buy-tag-service 和 xuele-group-buy-tag-api 不作为迁移依据。
```

## 4. 当前已经完成

新微服务项目路径：

```text
D:\Code4J\group-buy-microservice
```

当前已经完成：

```text
group-buy-tag-service/group-buy-tag-api
group-buy-tag-service/group-buy-tag-domain
```

### 4.1 POM 依赖原则

当前阶段只引入：

```text
group-buy-common-types
lombok
```

暂不引入：

```text
spring-context
spring-tx
spring-web
mybatis
redisson
dubbo
nacos
```

原因：

```text
api 只表达服务契约。
domain 只表达业务规则、领域模型、仓储端口。
外部技术后续分别进入 trigger、infrastructure、app。
```

### 4.2 API 契约

当前包结构：

```text
group-buy-tag-api/src/main/java/cn/xuele/api/tag
group-buy-tag-api/src/main/java/cn/xuele/api/tag/dto
```

已创建：

```text
ITagQueryService
TagQueryRequestDTO
TagQueryResponseDTO
```

当前建议：

```text
ITagQueryService 可以保留。
如果想让业务语义更准确，后续也可以改成 ICrowdTagQueryService。
```

命名说明：

```text
项目旧代码已有 IMarketTradeService、IDCCService、ITagService 等 I 前缀接口风格。
因此 ITagQueryService 在当前项目中是可接受的。
```

### 4.3 Domain 领域骨架

当前包结构：

```text
group-buy-tag-domain/src/main/java/cn/xuele/tag/domain/service
group-buy-tag-domain/src/main/java/cn/xuele/tag/domain/adapter
group-buy-tag-domain/src/main/java/cn/xuele/tag/domain/model/entity
```

已创建：

```text
ITagService
TagService
ITagRepository
CrowdTagsJobEntity
```

领域服务方法命名：

```text
boolean matchCrowdTag(String userId, String tagId)
void executeCrowdTagBatch(String tagId, String batchId)
```

命名原则：

```text
DomainService 命名偏业务动作。
Repository 端口命名偏领域需要的数据能力，但不暴露 MyBatis、Redis 等技术实现。
```

例如：

```text
matchCrowdTag 是业务动作。
executeCrowdTagBatch 是业务动作。
queryCrowdTagsJob 是领域需要查询标签任务。
saveCrowdTagUsers 是领域需要保存标签用户关系。
updateCrowdTagStatistics 是领域需要更新标签统计量。
```

不推荐在 domain 端口中使用：

```text
selectByTagId
insertCrowdTagsDetail
getRedisBitSet
```

这些名称过于贴近数据库或 Redis 实现。

## 5. 当前需要修正的小问题

### 5.1 DTO 的序列化字段

当前 DTO 中如果写的是：

```java
private final long serialVersion = 1L;
```

需要改成：

```java
@Serial
private static final long serialVersionUID = 1L;
```

并引入：

```java
import java.io.Serial;
```

原因：

```text
serialVersionUID 是 Java 序列化约定字段。
必须是 static final，字段名也应该固定为 serialVersionUID。
```

### 5.2 TagService.matchCrowdTag 需要接入仓储端口

当前如果仍然是：

```java
return false;
```

下一步应改为最小业务编排：

```java
if (userId == null || userId.isBlank() || tagId == null || tagId.isBlank()) {
    return false;
}
return tagRepository.isUserMatchedTag(userId, tagId);
```

注意：

```text
不要引入 Spring 的 StringUtils。
Java 21 自带 String#isBlank()，当前足够使用。
```

## 6. 回家后继续做什么

家环境路径：

```text
旧单体项目：E:\Code\Code4Java\xuele-group-buy
新微服务项目：E:\Code\Code4Java\group-buy-microservice
```

回家后的下一步建议按下面顺序继续。

### 6.1 先修正 API / Domain 小问题

完成：

```text
TagQueryRequestDTO / TagQueryResponseDTO 正确实现 Serializable
TagService.matchCrowdTag 接入 ITagRepository
```

然后检查 domain 纯净度：

```powershell
rg -n "org.springframework|org.apache.ibatis|org.redisson|org.apache.dubbo|RestController|Mapper|Redis|Transactional|@Service" E:\Code\Code4Java\group-buy-microservice\group-buy-tag-service\group-buy-tag-domain\src\main\java
```

期望：

```text
无输出。
```

### 6.2 再迁移 executeCrowdTagBatch 领域编排

参考旧单体：

```text
xuele-group-buy-domain/src/main/java/cn/xuele/domain/tag/service/TagService.java
```

迁移目标：

```text
查标签任务
-> 如果任务不存在，直接返回或后续抛业务异常
-> 暂时保留模拟用户列表
-> 保存标签用户
-> 更新标签统计量
```

注意：

```text
这里仍然不写 MyBatis / Redis / Spring 事务。
domain 只通过 ITagRepository 端口表达需要的能力。
```

### 6.3 再进入 infrastructure 迁移

完成 domain 后，下一阶段迁移：

```text
ICrowdTagsDao
ICrowdTagsDetailDao
ICrowdTagsJobDao
CrowdTags
CrowdTagsDetail
CrowdTagsJob
crowd_tags_mapper.xml
crowd_tags_detail_mapper.xml
crowd_tags_job_mapper.xml
TagRepository
Redis bitmap key / offset 计算
```

注意：

```text
旧单体里 crowd_tags_detail 的 XML 文件名是 IRefundOrderStrategy.xml，但 namespace 是 ICrowdTagsDetailDao。
迁移到新项目时建议改名为 crowd_tags_detail_mapper.xml。
```

## 7. 当前仍然不做的事情

暂不做：

```text
Dubbo Provider
Nacos 注册
HTTP Controller
定时 Job
MQ Listener
Activity / Trade 主链路改造
服务启动验收
```

原因：

```text
当前还处于 tag-service 内部能力迁移。
先把 api / domain / infrastructure 边界打干净，再暴露 RPC。
```

## 8. 面试表达

可以这样表达这一小阶段：

```text
我在迁移 tag-service 时没有直接复制旧单体代码，而是先按业务能力拆成标签生产线和标签查询线。

标签生产线负责根据标签批次任务圈选用户、落库并写入 Redis bitmap。
标签查询线负责根据 userId 和 tagId 判断用户是否命中人群标签，后续通过 Dubbo 提供给 activity-service 或 trade-service 调用。

第一步我先迁移 api 契约和 domain 领域端口，保证 domain 不依赖 Spring、MyBatis、Redis、Dubbo 等外部技术。
仓储接口用领域语言表达数据能力，具体 MySQL 和 Redis 实现后续放在 infrastructure。
```


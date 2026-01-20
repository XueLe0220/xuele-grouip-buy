package cn.xuele.infrastructure.adapter.repository;

import cn.xuele.domain.tag.adapter.repository.ITagRepository;
import cn.xuele.domain.tag.model.entity.CrowdTagsJobEntity;
import cn.xuele.infrastructure.dao.ICrowdTagsDao;
import cn.xuele.infrastructure.dao.ICrowdTagsDetailDao;
import cn.xuele.infrastructure.dao.ICrowdTagsJobDao;
import cn.xuele.infrastructure.dao.po.CrowdTags;
import cn.xuele.infrastructure.dao.po.CrowdTagsDetail;
import cn.xuele.infrastructure.dao.po.CrowdTagsJob;
import cn.xuele.types.common.RedisBitMapUtils;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBatch;
import org.redisson.api.RBitSet;
import org.redisson.api.RBitSetAsync;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签领域仓储实现类
 * <p>
 * 负责 Domain 层 Entity 与 Infrastructure 层 PO 之间的转换和持久化操作。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/02 21:52
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TagRepository implements ITagRepository {

    private final ICrowdTagsDao crowdTagsDao;
    private final ICrowdTagsDetailDao crowdTagsDetailDao;
    private final ICrowdTagsJobDao crowdTagsJobDao;
    private final RedissonClient redissonClient;

    // 定义 Key 前缀，防止与其他业务 Key 冲突
    private static final String BITMAP_KEY_PREFIX = "crowd:tag:bitmap:";

    @Override
    public CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId) {
        // 1. 构建查询 PO
        CrowdTagsJob crowdTagsJobReq = new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);

        // 2. 执行数据库查询
        CrowdTagsJob crowdTagsJob = crowdTagsJobDao.queryCrowdTagsJobEntity(crowdTagsJobReq);

        // 3. 空值检查，防止 NPE
        if (null == crowdTagsJob) {
            return null;
        }

        // 4. PO -> Domain Entity 转换
        return CrowdTagsJobEntity.builder()
                .tagType(crowdTagsJob.getTagType())
                .tagRule(crowdTagsJob.getTagRule())
                .statStartTime(crowdTagsJob.getStatStartTime())
                .statEndTime(crowdTagsJob.getStatEndTime())
                .build();
    }

    @Override
    public void updateCrowdTagStatistics(String tagId, int count) {
        // 1. 构建更新 PO
        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(count);

        // 2. 执行更新
        crowdTagsDao.updateCrowdTagStatistics(crowdTagsReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCrowdTagsUsers(String tagId, List<String> userIdList) {
        // 1. 防御性检查
        if (null == userIdList || userIdList.isEmpty()) {
            return;
        }

        // 2. 数据转换：指定 ArrayList 初始容量，避免频繁扩容
        List<CrowdTagsDetail> crowdTagsDetailReqList = new ArrayList<>(userIdList.size());
        for (String userId : userIdList) {
            CrowdTagsDetail crowdTagsDetail = CrowdTagsDetail.builder()
                    .tagId(tagId)
                    .userId(userId)
                    .build();
            crowdTagsDetailReqList.add(crowdTagsDetail);
        }

        // 3. 批量插入
        // 【重要】：请修改 SQL 为 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE
        // 这样就不需要 Java 层面的 try-catch 来吞掉异常了，且能保证部分成功
        crowdTagsDetailDao.addCrowdTagsUsers(crowdTagsDetailReqList);

        // 4. Redis 缓存优化 (使用 Pipeline/Batch)
        String cacheKey = BITMAP_KEY_PREFIX + tagId;

        // 创建批量操作对象
        RBatch batch = redissonClient.createBatch();
        // 获取异步的 BitSet 对象
        RBitSetAsync bitSetAsync = batch.getBitSet(cacheKey);

        for (String userId : userIdList) {
            // 这里只是将命令加入队列，并未发送网络请求
            long offset = RedisBitMapUtils.getIndexFromUserId(userId);
            log.info(">>>>>> 正在写入用户: [{}], 计算出的 Offset 是: [{}]", userId, offset);
            bitSetAsync.setAsync(offset, true);
        }

        try {
            // 一次性把所有命令发给 Redis 执行
            batch.execute();
        } catch (Exception e) {
            // 如果 Redis 失败，根据业务决定是否回滚 DB 或记录日志补偿
            log.error("人群标签 Redis 批量写入失败 tagId:{}", tagId, e);
            throw e;
        }
    }
}
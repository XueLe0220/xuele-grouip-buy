package cn.xuele.infrastructure.adapter.repository;

import cn.xuele.domain.tag.adapter.repository.ITagRepository;
import cn.xuele.domain.tag.model.entity.CrowdTagsJobEntity;
import cn.xuele.infrastructure.dao.ICrowdTagsDao;
import cn.xuele.infrastructure.dao.ICrowdTagsDetailDao;
import cn.xuele.infrastructure.dao.ICrowdTagsJobDao;
import cn.xuele.infrastructure.dao.po.CrowdTags;
import cn.xuele.infrastructure.dao.po.CrowdTagsDetail;
import cn.xuele.infrastructure.dao.po.CrowdTagsJob;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

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
    public void addCrowdTagsUsers(String tagId, List<String> userIdList) {
        // 1. 防御性检查：列表为空则直接返回，避免无效数据库交互
        if (null == userIdList || userIdList.isEmpty()) {
            return;
        }

        // 2. 数据转换：List<String> -> List<CrowdTagsDetail PO>
        List<CrowdTagsDetail> crowdTagsDetailReqList = new ArrayList<>();
        for (String userId : userIdList) {
            CrowdTagsDetail crowdTagsDetail = CrowdTagsDetail.builder()
                    .tagId(tagId)
                    .userId(userId)
                    .build();
            crowdTagsDetailReqList.add(crowdTagsDetail);
        }


        try {
            // 3. 批量插入
            crowdTagsDetailDao.addCrowdTagsUsers(crowdTagsDetailReqList);
            // redis 缓存
            String cacheKey = BITMAP_KEY_PREFIX + tagId;
            RBitSet bitSet = redissonClient.getBitSet(cacheKey);
            for (String userId : userIdList) {
                bitSet.set(getIndexFromUserId(userId), true);
            }
        } catch (DuplicateKeyException ignore) {
            // 暂时忽略唯一索引冲突
        }

    }

    @Override
    public boolean isUserInTag(String tagId, String userId) {
        try {
            String cacheKey = BITMAP_KEY_PREFIX + tagId;
            RBitSet bitSet = redissonClient.getBitSet(cacheKey);

            // 如果 Key 不存在（比如过期了），get 会返回 false，逻辑是安全的
            return bitSet.get(getIndexFromUserId(userId));
        } catch (Exception e) {
            log.error("【TagRepository】Redis Bitmap 查询失败，降级返回 false", e);
            return false;
        }
    }

    /**
     * [核心算法] 将 String 类型的 userId 映射为 Bitmap 的 offset
     * * @param userId 用户ID字符串
     *
     * @return 映射后的正整数索引
     */
    private long getIndexFromUserId(String userId) {
        int hash32 = Hashing.murmur3_32_fixed()
                .hashString(userId, StandardCharsets.UTF_8)
                .asInt();

        // 强制转化为正整数并返回
        int index = hash32 & 0x7FFFFFFF;

        return index % 100000000;
    }

}
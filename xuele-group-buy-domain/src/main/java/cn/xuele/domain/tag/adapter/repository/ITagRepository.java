package cn.xuele.domain.tag.adapter.repository;

import cn.xuele.domain.tag.model.entity.CrowdTagsJobEntity;

import java.util.List;

/**
 * 人群标签领域仓储接口
 * <p>
 * 定义了标签领域所需的持久化操作标准：
 * 1. 查询任务定义
 * 2. 更新统计数据
 * 3. 记录打标结果
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/02 21:51
 */
public interface ITagRepository {

    CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId);

    void updateCrowdTagStatistics(String tagId, int count);

    /**
     * 添加人群标签用户
     * <p>实现层需要同时处理 MySQL 落库和 Redis Bitmap 更新</p>
     */
    void addCrowdTagsUsers(String tagId, List<String> userIdList);

    /**
     * [新增] 判断用户是否命中该人群标签 (走 Redis Bitmap)
     * * @param tagId 人群ID
     * @param userId 用户ID
     * @return true-命中
     */
    boolean isUserInTag(String tagId, String userId);
}

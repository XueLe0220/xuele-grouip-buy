package cn.xuele.domain.tag.service;

/**
 * 人群标签领域服务接口
 * <p>
 * 定义标签领域对外的核心能力。
 * 目前主要用于触发人群标签的批处理任务。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 17:05
 */
public interface ITagService {

    /**
     * 执行人群标签批次任务
     *
     * @param tagId   标签ID
     * @param batchId 批次ID (用于区分同一种标签的不同次执行)
     */
    void execTagBatchJob(String tagId, String batchId);

}
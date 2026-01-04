package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.CrowdTagsJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群标签任务 DAO 接口
 * <p>
 * 对应表：crowd_tags_job
 * 负责查询任务的定义（如规则、类型、起止时间等）。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 16:59
 */
@Mapper
public interface ICrowdTagsJobDao {

    /**
     * 查询人群标签任务实体
     *
     * @param crowdTagsJobReq 查询条件（通常包含 tagId, batchId）
     * @return 任务 PO 对象，若不存在返回 null
     */
    CrowdTagsJob queryCrowdTagsJobEntity(CrowdTagsJob crowdTagsJobReq);
}
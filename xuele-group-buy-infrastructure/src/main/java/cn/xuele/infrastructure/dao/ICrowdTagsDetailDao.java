package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.CrowdTagsDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 人群标签明细 DAO 接口
 * <p>
 * 对应表：crowd_tags_detail
 * 负责批量存储被打标的用户 ID 明细。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/27 16:59
 */
@Mapper
public interface ICrowdTagsDetailDao {

    /**
     * 批量添加人群标签明细
     *
     * @param crowdTagsDetailReqList 明细 PO 对象列表
     */
    void addCrowdTagsUsers(List<CrowdTagsDetail> crowdTagsDetailReqList);
}
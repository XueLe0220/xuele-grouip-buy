package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 拼团主表 DAO 接口
 * <p>
 * 对应表：group_buy_order
 * 职责：管理拼团队伍的生命周期（创建、锁单、结算、完成）。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/06
 */
@Mapper
public interface IGroupBuyOrderDao {

    /**
     * 查询拼团进度详情
     *
     * @param groupBuyOrderReq 查询条件 (主要为 teamId)
     * @return 拼团单 PO 对象
     */
    GroupBuyOrder queryGroupBuyProgress(GroupBuyOrder groupBuyOrderReq);

    /**
     * 锁单：原子性扣减库存 (乐观锁)
     * <p>
     * 逻辑：lock_count + 1, 且需满足 (lock + complete) < target
     *
     * @param teamId 拼单组队ID
     * @return 1-锁单成功; 0-锁单失败(满员或不存在)
     */
    int updateAddLockCount(@Param("teamId") String teamId);

    /**
     * 新增拼团主单 (开团)
     *
     * @param groupBuyOrder 拼团单 PO
     */
    void insert(GroupBuyOrder groupBuyOrder);

    /**
     * 结算：原子性累加完成人数
     * <p>
     * 逻辑：complete_count + 1 (利用数据库行锁保证并发安全)
     *
     * @param teamId 拼单组队ID
     * @return 1-更新成功; 0-更新失败
     */
    int AddCompleteCount(@Param("teamId") String teamId);

    /**
     * 查询当前拼团已完成人数
     * <p>
     * 用途：在更新后进行双重校验 (Double Check)，判断是否撞线成团。
     *
     * @param teamId 拼单组队ID
     * @return 当前已完成人数
     */
    Integer queryGroupBuyTeamCompleteCountByTeamId(@Param("teamId") String teamId);

    /**
     * 更新拼团状态为完成 (COMPLETE)
     * <p>
     * 注意：应当包含 status=0 的条件以保证幂等性，防止重复触发。
     *
     * @param teamId 拼单组队ID
     * @return 1-更新成功(撞线); 0-更新失败(已完成或状态不符)
     */
    int updateTeamStatus2COMPLETE(@Param("teamId") String teamId);

    /**
     * 查询拼团领域实体
     *
     * @param teamId 拼单组队ID
     * @return 拼团主表实体 (用于构建聚合根)
     */
    GroupBuyOrder queryGroupBuyTeamByTeamId(@Param("teamId") String teamId);

    int unpaid2Refund(GroupBuyOrder groupBuyOrderReq);

    int paidUnformed2Refund(GroupBuyOrder groupBuyOrderReq);

    int paidFormed2Refund(GroupBuyOrder groupBuyOrderReq);
}
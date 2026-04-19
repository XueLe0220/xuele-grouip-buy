package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

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

    GroupBuyOrder queryGroupBuyProgress(GroupBuyOrder groupBuyOrderReq);

    int updateAddLockCount(@Param("teamId") String teamId);

    void insert(GroupBuyOrder groupBuyOrder);

    int AddCompleteCount(@Param("teamId") String teamId);

    Integer queryGroupBuyTeamCompleteCountByTeamId(@Param("teamId") String teamId);

    int updateTeamStatus2COMPLETE(@Param("teamId") String teamId);

    GroupBuyOrder queryGroupBuyTeamByTeamId(@Param("teamId") String teamId);

    int unpaid2Refund(GroupBuyOrder groupBuyOrderReq);

    int paidUnformed2Refund(GroupBuyOrder groupBuyOrderReq);

    int paidFormed2Refund(GroupBuyOrder groupBuyOrderReq);

    List<GroupBuyOrder> queryGroupBuyTeamByTeamIds(@Param("teamIds") Set<String> teamIds);
}
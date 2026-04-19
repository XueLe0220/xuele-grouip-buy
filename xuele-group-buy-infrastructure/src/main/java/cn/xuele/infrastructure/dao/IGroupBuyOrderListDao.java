package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拼团订单明细 DAO 接口
 * <p>
 * 对应表：group_buy_order_list
 * 职责：管理用户维度的交易契约（个人订单）。
 * 作用：记录谁(User)在哪个团(Team)买了什么(Goods)，作为后续履约发货的凭证。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07
 */
@Mapper
public interface IGroupBuyOrderListDao {

    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

    void insert(GroupBuyOrderList groupBuyOrderListReq);

    Integer queryOrderCountByActivityId(GroupBuyOrderList groupBuyOrderListReq);

    Integer updateOrderStatus2COMPLETE(GroupBuyOrderList groupBuyOrderListReq);

    List<String> queryGroupBuyCompleteOrderOutTradeNoListByTeamId(@Param("teamId") String teamId);

    int unpaid2Refund(GroupBuyOrderList groupBuyOrderListReq);

    int paidUnformed2Refund(GroupBuyOrderList groupBuyOrderListReq);

    int paidFormed2Refund(GroupBuyOrderList groupBuyOrderListReq);

    List<GroupBuyOrderList> queryTimeoutUnpaidOrderList();
}
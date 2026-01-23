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

    /**
     * 根据外部单号查询订单记录
     * <p>
     * 用途：在锁单前进行幂等性校验，防止同一笔外部交易重复下单。
     *
     * @param groupBuyOrderListReq 查询条件 (必须包含 userId 和 outTradeNo)
     * @return 订单明细 PO; 若不存在则返回 null
     */
    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 写入拼团明细记录 (落单)
     * <p>
     * 注意：通常依赖数据库唯一索引 (bizId 或 outTradeNo) 进行最终的防重兜底。
     *
     * @param groupBuyOrderListReq 待插入的订单详情 PO
     */
    void insert(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 统计用户在指定活动下的已参与次数
     * <p>
     * 用途：活动限购规则校验 (例如：限制每个用户 ID 仅能参与 3 次)。
     *
     * @param groupBuyOrderListReq 查询条件 (必须包含 activityId, userId)
     * @return 该用户在该活动下的订单数量
     */
    Integer queryOrderCountByActivityId(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 更新订单状态为已完成 (COMPLETE)
     * <p>
     * 逻辑：根据 userId + outTradeNo 更新状态。
     * 建议：SQL 中应包含 status=WAIT_PAY 条件，确保状态流转的合法性。
     *
     * @param groupBuyOrderListReq 更新条件
     * @return 1-更新成功; 0-更新失败(订单不存在或已支付)
     */
    Integer updateOrderStatus2COMPLETE(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 查询某团下所有已完成的外部单号
     * <p>
     * 用途：拼团成功后，聚合该团所有成员的外部单号，用于生成发货通知或回调商户。
     *
     * @param teamId 拼单组队ID
     * @return 外部单号列表 (out_trade_no list)
     */
    List<String> queryGroupBuyCompleteOrderOutTradeNoListByTeamId(@Param("teamId") String teamId);

    int unpaid2Refund(GroupBuyOrderList groupBuyOrderListReq);
}
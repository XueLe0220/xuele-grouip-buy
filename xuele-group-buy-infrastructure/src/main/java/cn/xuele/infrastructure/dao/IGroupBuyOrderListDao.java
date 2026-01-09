package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户拼单明细 DAO 接口
 * <p>
 * 对应表：group_buy_order_list
 * 职责：管理用户维度的交易契约。
 * 在锁单业务中，这张表的落库成功，代表了“用户成功抢到了一个位置”。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 00:28
 */
@Mapper
public interface IGroupBuyOrderListDao {

    /**
     * 根据外部交易单号查询订单记录
     * <p>
     * 核心作用：幂等性校验
     * 业务场景：
     * 当用户发起锁单请求时，首先根据 userId + outTradeNo 查询是否已经存在未支付的订单。
     * 如果存在，说明是重复请求，直接返回已有订单，不再重复扣减库存。
     *
     * @param groupBuyOrderListReq 查询条件（通常包含 userId 和 outTradeNo）
     * @return 存在的订单记录，不存在则返回 null
     */
    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 插入新的拼单记录
     * <p>
     * 核心作用：**落地锁单 (Finalize Lock)**
     * 业务场景：
     * 在 group_buy_order (主表) 的 lock_count 更新成功后，
     * 必须在此表中插入一条记录，作为用户的“入场券”。
     * <p>
     * 注意：
     * XML 中通常利用数据库的唯一索引 (Unique Key on order_id 或 out_trade_no)
     * 来作为最后一道防线。如果插入抛出 DuplicateKeyException，说明发生并发冲突。
     *
     * @param groupBuyOrderListReq 待插入的订单详情 PO 对象
     */
    void insert(GroupBuyOrderList groupBuyOrderListReq);
}
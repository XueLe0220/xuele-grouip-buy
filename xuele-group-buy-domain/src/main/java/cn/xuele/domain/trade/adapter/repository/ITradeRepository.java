package cn.xuele.domain.trade.adapter.repository;

import cn.xuele.domain.trade.model.aggregate.GroupBuyLockOrderAggregate;
import cn.xuele.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;

/**
 * 交易仓储接口
 * <p>
 * DDD 架构定位：
 * 位于领域层 (Domain Layer) 的适配器接口。
 * 作用是将领域对象（Aggregate/Entity）的持久化逻辑与底层的数据库实现解耦。
 * 基础设施层 (Infrastructure) 必须实现此接口。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:14
 */
public interface ITradeRepository {

    /**
     * 根据外部单号查询未支付的订单
     * <p>
     * 业务意图：**幂等性校验 (Idempotency Check)**
     * 在发起锁单前，先检查该用户针对此交易单号是否已有未完成的订单。
     * 如果有，直接返回旧订单，避免重复扣减库存。
     *
     * @param userId     用户ID
     * @param outTradeNo 外部交易单号 (唯一键)
     * @return 存在的订单实体，不存在则返回 null
     */
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    /**
     * 查询拼团进度
     * <p>
     * 业务意图：**快速失败 (Fail-Fast) & 容量检查**
     * 在尝试去抢占坑位（写操作）之前，先读取当前团的状态。
     * 如果已满员，直接在 Service 层拦截，减少数据库写锁的竞争。
     *
     * @param teamId 拼单组队ID
     * @return 拼团进度值对象 (包含目标数、已锁数、已完成数)
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 锁单核心方法 (持久化聚合根)
     * <p>
     * 业务意图：**事务性落库**
     * 将组装好的聚合根 (User + Activity + Discount) 保存到数据库。
     * 内部包含两个原子操作：
     * 1. 扣减库存/占用坑位 (update group_buy_order)
     * 2. 生成订单明细 (insert group_buy_order_list)
     *
     * @param groupBuyOrderAggregate 拼团订单聚合根
     * @return 锁单成功后生成的订单实体 (包含生成的 orderId)
     */
    MarketPayOrderEntity lockMarketPayOrder(GroupBuyLockOrderAggregate groupBuyOrderAggregate);

    GroupBuyActivityEntity queryGroupBuyActivityByActivityId(Long activityId);

    Integer queryOrderCountByActivityIdAndUserId(Long activityId, String userId);

    void settlement(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    boolean isSCBlackIntercept(String source, String channel);

    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);
}
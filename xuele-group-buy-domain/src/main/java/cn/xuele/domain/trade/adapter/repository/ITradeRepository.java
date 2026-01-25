package cn.xuele.domain.trade.adapter.repository;

import cn.xuele.domain.trade.model.aggregate.GroupBuyLockOrderAggregate;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

/**
 * 交易仓储接口
 * <p>
 * DDD 架构定位：领域层 (Domain) 接口，负责聚合根/实体的持久化，解耦基础设施层。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:14
 */
public interface ITradeRepository {

    /**
     * 根据外部单号查询未支付订单（幂等性校验）
     */
    MarketPayOrderEntity queryGroupBuyOrderRecordByOutTradeNo(String userId, String outTradeNo);

    /**
     * 查询拼团当前进度（快速失败/容量检查）
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 锁单核心方法：持久化聚合根（扣减DB库存 + 生成订单明细）
     */
    MarketPayOrderEntity lockMarketPayOrder(GroupBuyLockOrderAggregate groupBuyOrderAggregate);

    /**
     * 查询拼团活动配置详情
     */
    GroupBuyActivityEntity queryGroupBuyActivityByActivityId(Long activityId);

    /**
     * 统计用户在该活动下的已下单数量（用于限购规则校验）
     */
    Integer queryOrderCountByActivityIdAndUserId(Long activityId, String userId);

    /**
     * 执行拼团结算逻辑（生成回调通知任务）
     */
    NotifyTaskEntity settlement(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    /**
     * 渠道(Source)与来源(Channel)的黑名单拦截校验
     */
    boolean isSCBlackIntercept(String source, String channel);

    /**
     * 根据ID查询拼团组队详情
     */
    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    /**
     * 扫描全部未执行的拼团结算通知任务
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    /**
     * 查询指定拼团ID下的未执行通知任务
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);

    /**
     * 更新任务状态：执行成功
     */
    int updateNotifyTaskStatusSuccess(String teamId);

    /**
     * 更新任务状态：执行失败（不再重试）
     */
    int updateNotifyTaskStatusError(String teamId);

    /**
     * 更新任务状态：准备重试（增加重试计数）
     */
    int updateNotifyTaskStatusRetry(String teamId);

    /**
     * 缓存层抢占组队库存（Redis原子递增，无锁化设计）
     * @return true=抢占成功, false=库存不足
     */
    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer targetCount, Integer validTime);

    /**
     * 缓存库存回补（用于DB锁单失败后的事务补偿）
     * <p>注意：此方法在你上一段Filter代码中被调用，接口中需补充定义</p>
     */
    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);

    NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paidUnformed2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    NotifyTaskEntity paidFormed2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);
}
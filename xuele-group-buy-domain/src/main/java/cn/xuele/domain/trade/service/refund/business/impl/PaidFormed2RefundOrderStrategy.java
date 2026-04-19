package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 发起退单（已成团&已支付），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:54
 */
@Slf4j
@Service("paidTeam2RefundStrategy")
public class PaidFormed2RefundOrderStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；已支付，已成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(tradeRefundOrderEntity.getTeamId());
        Integer completeCount = groupBuyTeamEntity.getCompleteCount();

        // 最后一笔也退单，则更新拼团订单为失败
        GroupBuyTeamStatusVO groupBuyTeamStatus = 1 == completeCount ? GroupBuyTeamStatusVO.FAIL : GroupBuyTeamStatusVO.COMPLETE_FAIL;

        // 1. 退单，已支付&已成团
        NotifyTaskEntity notifyTaskEntity = repository.paidFormed2Refund(GroupBuyRefundAggregate.buildPaidFormed2RefundAggregate(tradeRefundOrderEntity, -1, -1, groupBuyTeamStatus));

        // 2. 发送MQ消息 - 发送MQ，恢复锁单库存量使用
        sendRefundNotifyMessage(notifyTaskEntity, "已支付，已成团");

    }

    @Override
    public void reverseStock(TeamRefundSuccessEvent teamRefundSuccessEvent) throws Exception {
        log.info("退单；已支付、已成团，队伍组队结束，不需要恢复锁单量 {} {} {}", teamRefundSuccessEvent.getUserId(), teamRefundSuccessEvent.getActivityId(), teamRefundSuccessEvent.getTeamId());
    }

}
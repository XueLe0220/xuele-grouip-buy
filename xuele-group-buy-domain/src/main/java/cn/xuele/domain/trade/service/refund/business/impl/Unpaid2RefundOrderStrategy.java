package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 退单策略：未支付取消
 * <p>
 * 场景：用户锁单后未付款，主动取消或超时自动取消。
 * 动作：修改订单状态 + 释放拼团锁单名额。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:41
 */
@Slf4j
@Service("unpaid2RefundStrategy")
@RequiredArgsConstructor
public class Unpaid2RefundOrderStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());
        // 1. 退单；未支付，未成团
        NotifyTaskEntity notifyTaskEntity = repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));

        // 2. 发送MQ消息 - 发送MQ，恢复锁单库存量使用
        sendRefundNotifyMessage(notifyTaskEntity, "未支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccessEvent teamRefundSuccessEvent) throws Exception {
        doReverseStock(teamRefundSuccessEvent, "未支付，未成团，但有锁单记录，要恢复锁单库存");
    }

}
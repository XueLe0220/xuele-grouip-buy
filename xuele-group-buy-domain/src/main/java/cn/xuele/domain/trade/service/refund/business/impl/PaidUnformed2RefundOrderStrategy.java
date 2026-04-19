package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.valobj.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 发起退单（未成团&已支付），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:54
 */
@Slf4j
@Service("paid2RefundStrategy")
public class PaidUnformed2RefundOrderStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception {
        log.info("退单；已支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        // 1. 退单，已支付&未成团
        NotifyTaskEntity notifyTaskEntity = repository.paidUnformed2Refund(GroupBuyRefundAggregate.buildPaidUnformed2RefundAggregate(tradeRefundOrderEntity, -1, -1));

        // 2. 发送MQ消息 - 发送MQ，恢复锁单库存量使用
        sendRefundNotifyMessage(notifyTaskEntity, "已支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccessEvent teamRefundSuccessEvent) throws Exception {
        doReverseStock(teamRefundSuccessEvent, "已支付，未成团，但有锁单记录，要恢复锁单库存");
    }

}


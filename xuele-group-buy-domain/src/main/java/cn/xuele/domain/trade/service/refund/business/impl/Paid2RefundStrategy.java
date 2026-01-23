package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
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
public class Paid2RefundStrategy implements IRefundStrategy {
    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {

    }
}

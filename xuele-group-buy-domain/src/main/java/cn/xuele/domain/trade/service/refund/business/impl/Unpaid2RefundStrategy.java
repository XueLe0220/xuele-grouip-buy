package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:41
 */
@Slf4j
@Service("unpaid2RefundStrategy")
@RequiredArgsConstructor
public class Unpaid2RefundStrategy implements IRefundStrategy {

    private final ITradeRepository repository;

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}",
                tradeRefundOrderEntity.getUserId(),
                tradeRefundOrderEntity.getTeamId(),
                tradeRefundOrderEntity.getOrderId());

        //TODO
        repository.unpaid2Refund(GroupBuyRefundAggregate.bulidUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));
    }

}

package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
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
public class Unpaid2RefundStrategy implements IRefundStrategy {

    private final ITradeRepository repository;

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单策略(未支付取消)开始 userId:{} teamId:{} orderId:{}",
                tradeRefundOrderEntity.getUserId(),
                tradeRefundOrderEntity.getTeamId(),
                tradeRefundOrderEntity.getOrderId());

        // 执行数据库操作：传入 -1 表示释放 1 个锁单坑位
        repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));
    }

}
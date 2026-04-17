package cn.xuele.domain.trade.service.refund.business;

import cn.xuele.domain.trade.model.entity.TeamRefundEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;

/**
 * 退单策略接口
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:38
 */
public interface IRefundStrategy {

    void refund(TradeRefundOrderEntity tradeRefundOrderEntity);

    void reverseStock(TeamRefundEvent teamRefundEvent) throws Exception;
}

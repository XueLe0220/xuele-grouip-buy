package cn.xuele.domain.trade.service.refund;

import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:27
 */
public interface ITradeRefundOrderService {
    TradeRefundBehaviorEntity refund(TradeRefundCommandEntity tradeRefundCommandEntity);
}

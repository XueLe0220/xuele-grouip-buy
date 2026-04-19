package cn.xuele.domain.trade.service.refund.filter;

import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;
import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.xuele.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 重复退单检查
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/04/19 15:43
 */
@Slf4j
@Service
public class UniqueRefundNodeFilter implements ILogicHandler<TradeRefundCommandEntity,
        TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity,
                                           TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，重复退单检查 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(),
                tradeRefundCommandEntity.getOutTradeNo());

        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();

        // 返回幂等，已完成退单
        if (TradeOrderStatusEnumVO.CLOSE.equals(tradeOrderStatusEnumVO)) {
            log.info("逆向流程，退单操作(幂等-重复退单) userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(),
                    tradeRefundCommandEntity.getOutTradeNo());
            return TradeRefundBehaviorEntity.builder()
                    .userId(tradeRefundCommandEntity.getUserId())
                    .orderId(marketPayOrderEntity.getOrderId())
                    .teamId(marketPayOrderEntity.getTeamId())
                    .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.REPEAT)
                    .build();
        }

        return next(tradeRefundCommandEntity, dynamicContext);
    }
}

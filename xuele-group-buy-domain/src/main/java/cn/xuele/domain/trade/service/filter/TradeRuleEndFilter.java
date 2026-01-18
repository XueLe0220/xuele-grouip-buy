package cn.xuele.domain.trade.service.filter;

import cn.xuele.domain.trade.model.entity.TradeRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeRuleFilterBackEntity;
import cn.xuele.domain.trade.service.fatcory.TradeRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 15:26
 */
public class TradeRuleEndFilter implements ILogicHandler<TradeRuleCommandEntity,
        TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> {
    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        return TradeRuleFilterBackEntity.builder()
                .userTakeOrderCount(dynamicContext.getUserTakeCount())
                .build();
    }
}

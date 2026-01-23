package cn.xuele.domain.trade.service.lock.filter;

import cn.xuele.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.xuele.domain.trade.service.lock.fatcory.TradeLockRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;

/**
 * 交易规则过滤链 - 终止节点
 * 该节点位于链路末端，负责汇总上下文信息并构建最终返回对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 15:26
 */
public class TradeRuleEndFilter implements ILogicHandler<TradeLockRuleCommandEntity,
        TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        // 规则链执行完毕，将上下文中计算/获取的结果（如：用户参与次数）封装到返回实体中
        return TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(dynamicContext.getUserTakeCount())
                .recoveryTeamStockKey(dynamicContext.getRecoveryTeamStockKey())
                .build();
    }

}
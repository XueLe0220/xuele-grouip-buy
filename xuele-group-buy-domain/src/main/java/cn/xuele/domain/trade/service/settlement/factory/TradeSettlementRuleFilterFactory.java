package cn.xuele.domain.trade.service.settlement.factory;

import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.xuele.domain.trade.service.settlement.filter.TradeSettlementEndRuleFilter;
import cn.xuele.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import cn.xuele.domain.trade.service.settlement.filter.SCRuleFilter;
import cn.xuele.domain.trade.service.settlement.filter.SettableRuleFilter;
import cn.xuele.types.design.framework.link.LinkArmory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 交易结算规则过滤链工厂
 * <p>
 * 作用：组装 Filter 节点，构建责任链对象，并将其注册为 Spring Bean。
 * 使用时直接注入 BusinessLinkedList 即可。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Configuration
public class TradeSettlementRuleFilterFactory {

    @Bean("tradeSettlementRuleFilter")
    public BusinessLinkedList<TradeSettlementRuleCommandEntity, DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter(
            SCRuleFilter scRuleFilter,
            OutTradeNoRuleFilter outTradeNoRuleFilter,
            SettableRuleFilter settableRuleFilter
            // 注意：这里不再注入 EndRuleFilter，因为我们决定手动 new
    ) {
        // 组装链条：SC -> OutTradeNo -> Settable -> End
        LinkArmory<TradeSettlementRuleCommandEntity, DynamicContext, TradeSettlementRuleFilterBackEntity> linkArmory
                = new LinkArmory<>(
                "交易结算规则过滤链",
                scRuleFilter,
                outTradeNoRuleFilter,
                settableRuleFilter,
                new TradeSettlementEndRuleFilter()
        );

        return linkArmory.getLogicLink();
    }

    /**
     * 动态上下文
     * 用于在责任链的各个节点之间传递“中间产物”（如查出来的 Entity）
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private GroupBuyTeamEntity groupBuyTeamEntity;
        private MarketPayOrderEntity marketPayOrderEntity;
    }
}
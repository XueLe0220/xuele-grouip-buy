package cn.xuele.domain.trade.service.fatcory;

import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.TradeRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeRuleFilterBackEntity;
import cn.xuele.domain.trade.service.filter.ActivityUsabilityRuleFilter;
import cn.xuele.domain.trade.service.filter.TradeRuleEndFilter;
import cn.xuele.domain.trade.service.filter.UserTakeLimitRuleFilter;
import cn.xuele.types.design.framework.link.LinkArmory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:35
 */
@Slf4j
@Service
public class TradeRuleFilterFactory {

    @Bean("tradeRuleFilter")
    BusinessLinkedList<TradeRuleCommandEntity, DynamicContext, TradeRuleFilterBackEntity> tradeRuleFilter(ActivityUsabilityRuleFilter activityUsabilityRuleFilter, UserTakeLimitRuleFilter userTakeLimitRuleFilter) {
        LinkArmory<TradeRuleCommandEntity, DynamicContext, TradeRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链",
                        activityUsabilityRuleFilter, userTakeLimitRuleFilter, new TradeRuleEndFilter());
        return linkArmory.getLogicLink();
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        private GroupBuyActivityEntity groupBuyActivityEntity;
        private Integer userTakeCount;

    }
}

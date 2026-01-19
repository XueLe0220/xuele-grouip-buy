package cn.xuele.domain.trade.service.lock.fatcory;

import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.xuele.domain.trade.service.lock.filter.ActivityUsabilityRuleFilter;
import cn.xuele.domain.trade.service.lock.filter.TradeRuleEndFilter;
import cn.xuele.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import cn.xuele.types.design.framework.link.LinkArmory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * 交易规则过滤链工厂
 * 用于组装拼团交易过程中的各类审核规则（如活动可用性、限购次数等）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:35
 */
@Configuration
public class TradeLockRuleFilterFactory {

    /**
     * 构造交易规则过滤链
     * 包含：活动可用性校验 -> 个人限购校验 -> 链路结束节点
     */
    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeLockRuleCommandEntity, DynamicContext, TradeLockRuleFilterBackEntity> tradeRuleFilter(
            ActivityUsabilityRuleFilter activityUsabilityRuleFilter,
            UserTakeLimitRuleFilter userTakeLimitRuleFilter) {

        LinkArmory<TradeLockRuleCommandEntity, DynamicContext, TradeLockRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链",
                        activityUsabilityRuleFilter,
                        userTakeLimitRuleFilter,
                        new TradeRuleEndFilter());

        return linkArmory.getLogicLink();
    }

    /**
     * 交易规则过滤链上下文
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /** 拼团活动实体 */
        private GroupBuyActivityEntity groupBuyActivityEntity;

        /** 用户当前已参与次数 */
        private Integer userTakeCount;

    }
}
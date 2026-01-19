package cn.xuele.domain.trade.service.settlement.filter;

import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算规则过滤器 - 结束节点 (Assembler)
 * <p>
 * 职责：
 * 1. 作为责任链的终点，负责“收口”。
 * 2. 从上下文 (Context) 中提取前置节点查询到的数据。
 * 3. 解决对象类型转换 (如 Date -> LocalDateTime)，组装最终返回值。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Slf4j
@Service
public class TradeSettlementEndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity,
        TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter,
                                                     TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        // 1. 日志记录：标记流程结束
        log.info("结算规则过滤-结束节点 exec. userId:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 2. 获取上下文对象
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();

        // 3. 返回封装数据 (Data Transfer)
        return TradeSettlementRuleFilterBackEntity.builder()
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .targetCount(groupBuyTeamEntity.getTargetCount())
                .completeCount(groupBuyTeamEntity.getCompleteCount())
                .lockCount(groupBuyTeamEntity.getLockCount())
                .status(groupBuyTeamEntity.getStatus())
                .validStartTime(groupBuyTeamEntity.getValidStartTime())
                .validEndTime(groupBuyTeamEntity.getValidEndTime())
                .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
                .build();
    }
}
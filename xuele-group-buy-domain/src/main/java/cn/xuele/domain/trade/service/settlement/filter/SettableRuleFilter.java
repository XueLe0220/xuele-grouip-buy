package cn.xuele.domain.trade.service.settlement.filter;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 结算规则过滤器 - 交易时效性校验
 * <p>
 * 职责：校验外部交易时间是否在拼团有效期内。
 * 注意：这是校验链的一环，后续还有 EndNode 负责组装数据。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettableRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    private final ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {

        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        String teamId = marketPayOrderEntity.getTeamId();

        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(teamId);

        LocalDateTime outTradeTime = requestParameter.getOutTradeTime();
        // 判断，外部交易时间，要小于拼团结束时间。否则抛异常。
        if (!outTradeTime.isBefore(groupBuyTeamEntity.getValidEndTime())) {
            log.error("订单交易时间不在拼团有效时间范围内");
            throw new AppException(ResponseCode.E0106);
        }

        // 4. 将查到的团信息放入上下文，供 EndFilter 使用
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);

        // 5. 放行给 EndFilter
        return next(requestParameter, dynamicContext);
    }
}
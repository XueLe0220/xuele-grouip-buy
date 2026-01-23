package cn.xuele.domain.trade.service.settlement.filter;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算规则过滤器 - 外部交易单号校验
 * <p>
 * 职责：
 * 1. 校验单号有效性：确认该外部单号对应的“待支付”订单是否存在。
 * 2. 数据上下文装载：将查询到的订单实体注入 DynamicContext，供后续链路使用。
 * 对应规则：Rule 2 - OutTradeNo Check & Data Loading
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutTradeNoRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    private final ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        // 1. 日志记录
        log.info("结算规则校验-外部单号校验 check start. userId:{} outTradeNo:{}",
                requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 2. 查询数据库
        MarketPayOrderEntity marketPayOrderEntity = repository.queryGroupBuyOrderRecordByOutTradeNo(
                requestParameter.getUserId(),
                requestParameter.getOutTradeNo()
        );

        // 3. 校验逻辑：订单不存在 或 状态不正确
        if (null == marketPayOrderEntity || TradeOrderStatusEnumVO.CLOSE.equals(marketPayOrderEntity.getTradeOrderStatus())) {
            log.error("不存在的外部交易单号或用户已退单，不需要做支付订单结算:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());
            throw new AppException(ResponseCode.E0104);
        }

        // 4. 核心动作：上下文传递 (Context Passing)
        // 将查到的实体存入上下文，这样后续的过滤器（如时间校验）就不用再次查库了
        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);

        // 5. 放行
        return next(requestParameter, dynamicContext);
    }
}
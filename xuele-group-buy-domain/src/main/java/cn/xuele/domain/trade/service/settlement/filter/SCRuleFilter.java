package cn.xuele.domain.trade.service.settlement.filter;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算规则过滤器 - 渠道/来源黑名单校验
 * <p>
 * 职责：作为结算链路的第一道防线，拦截非法或未授权的渠道来源。
 * 对应规则：Rule 1 - SC (Source/Channel) Blacklist Check
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SCRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity,
        TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    private final ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter,
                                                     TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        // 1. 日志记录：保留关键链路信息 (UserId, TradeNo)，方便排查
        log.info("结算规则校验-黑名单(SC) check start. userId:{} outTradeNo:{} source:{} channel:{}",
                requestParameter.getUserId(), requestParameter.getOutTradeNo(),
                requestParameter.getSource(), requestParameter.getChannel());

        // 2. 校验逻辑：查询数据库配置
        boolean intercept = repository.isSCBlackIntercept(requestParameter.getSource(), requestParameter.getChannel());

        // 3. 拦截处理
        if (intercept) {
            // 使用 WARN 级别，因为这是业务规则拒绝，并非系统异常
            log.warn("结算规则校验-黑名单拦截. userId:{} source:{} channel:{}",
                    requestParameter.getUserId(), requestParameter.getSource(), requestParameter.getChannel());
            throw new AppException(ResponseCode.E0105);
        }

        // 4. 放行：执行链路的下一个节点
        return next(requestParameter, dynamicContext);
    }
}
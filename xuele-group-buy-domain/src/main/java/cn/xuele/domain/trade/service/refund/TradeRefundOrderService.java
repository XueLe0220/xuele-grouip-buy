package cn.xuele.domain.trade.service.refund;

import cn.xuele.domain.trade.model.entity.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;
import cn.xuele.domain.trade.model.valobj.RefundTypeEnumVO;
import cn.xuele.domain.trade.service.ITradeRefundOrderService;
import cn.xuele.domain.trade.service.refund.business.IRefundOrderStrategy;
import cn.xuele.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 交易逆向流程服务
 * <p>
 * 核心职责：处理各类退单请求（未支付取消、拼团失败退款等），
 * 通过策略模式路由到具体的执行逻辑。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:27
 */
@Slf4j
@Service
public class TradeRefundOrderService implements ITradeRefundOrderService {

    private final Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    public TradeRefundOrderService(Map<String, IRefundOrderStrategy> refundOrderStrategyMap) {
        this.refundOrderStrategyMap = refundOrderStrategyMap;
    }

    @Resource
    private BusinessLinkedList<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> tradeRefundRuleFilter;

    @Override
    public TradeRefundBehaviorEntity refund(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception {
        log.info("逆向流程，退单操作 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        return tradeRefundRuleFilter.apply(tradeRefundCommandEntity, new TradeRefundRuleFilterFactory.DynamicContext());
    }

    @Override
    public void restoreTeamLockStock(TeamRefundSuccessEvent teamRefundSuccessEvent) throws Exception {
        log.info("逆向流程，恢复锁单量 userId:{} activityId:{} teamId:{}", teamRefundSuccessEvent.getUserId(), teamRefundSuccessEvent.getActivityId(), teamRefundSuccessEvent.getTeamId());
        String type = teamRefundSuccessEvent.getType();

        // 根据枚举值获取对应的退单类型
        RefundTypeEnumVO refundTypeEnumVO = RefundTypeEnumVO.getRefundTypeEnumVOByCode(type);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundTypeEnumVO.getStrategy());

        // 逆向库存操作，恢复锁单量
        refundOrderStrategy.reverseStock(teamRefundSuccessEvent);
    }

}

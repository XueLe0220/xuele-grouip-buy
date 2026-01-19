package cn.xuele.domain.trade.service.settlement;

import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementEntity;

/**
 * 交易结算领域服务接口
 * <p>
 * 职责：负责接收支付渠道的通知，完成拼团订单的最终结算（扣减库存/修改状态/成团判定）。
 *
 * @author XueLe
 * @since 2026/01/18
 */
public interface ITradeSettlementOrderService {

    /**
     * 结算营销拼团订单
     * <p>
     * 1. 幂等性保障：支持重复调用，重复调用时应直接返回成功结果或当前最新状态。
     * 2. 资金校验：必须校验回调金额与订单金额是否一致。
     * 3. 事务性：方法执行成功即代表数据库落库完成。
     *
     * @param tradePaySettlementEntity 支付回调上下文 (包含外部单号、金额、支付时间)
     * @return TradeSettlementEntity 结算回执 (包含内部单号、是否成团)
     */
    TradeSettlementEntity settlement(TradePaySettlementEntity tradePaySettlementEntity) throws Exception;

}
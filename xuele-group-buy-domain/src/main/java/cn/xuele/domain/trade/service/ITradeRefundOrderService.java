package cn.xuele.domain.trade.service;

import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;

/**
 * 交易退单应用服务接口
 * <p>
 * 定义逆向交易流程的标准入口。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:27
 */
public interface ITradeRefundOrderService {

    /**
     * 执行退单
     *
     * @param tradeRefundCommandEntity 退单指令对象(含外部单号、用户ID)
     * @return 退单处理结果(包含处理状态 SUCCESS/REPEAT/FAIL)
     */
    TradeRefundBehaviorEntity refund(TradeRefundCommandEntity tradeRefundCommandEntity);
}
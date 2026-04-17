package cn.xuele.domain.trade.model.entity;

import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 营销支付订单实体
 * <p>
 * 领域含义：交易上下文的核心聚合根。
 * 作用：代表一笔经过营销计算、已完成“锁单”且待支付的交易单。
 *
 * @author XueLe
 * @since 2026/01/07
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPayOrderEntity {

    /** 拼单组队ID */
    private String teamId;
    /** 预购订单ID */
    private String orderId;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 支付金额 */
    private BigDecimal payPrice;
    /** 交易订单状态枚举 */
    private TradeOrderStatusEnumVO tradeOrderStatusEnumVO;

}
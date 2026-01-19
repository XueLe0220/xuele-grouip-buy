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

    /** 拼团团队ID */
    private String teamId;

    /**
     * 预购订单ID (聚合根标识)
     * <p>
     * 对应 group_buy_order_list.order_id，也是支付渠道的关联凭证。
     */
    private String orderId;

    /** 优惠金额 (营销计算结果) */
    private BigDecimal deductionPrice;

    /**
     * 交易订单状态
     * <p>
     * 描述订单生命周期 (如：创建锁单、支付完成、交易关闭)。
     */
    private TradeOrderStatusEnumVO tradeOrderStatus;

}
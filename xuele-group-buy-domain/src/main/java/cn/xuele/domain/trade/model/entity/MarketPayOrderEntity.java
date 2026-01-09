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
 * 领域定义：
 * 它是交易上下文（Trade Context）中的核心聚合根或核心实体。
 * 代表了一笔已经经过营销规则计算、并完成“锁单”动作的待支付交易单。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPayOrderEntity {

    /**
     * 预购订单ID (唯一标识)
     * <p>
     * 实体的身份ID。对应数据库 group_buy_order_list 中的 order_id。
     * 也是后续唤起支付渠道（微信/支付宝）时的关联凭证。
     */
    private String orderId;

    /**
     * 折扣金额
     * <p>
     * 记录这笔订单享受了多少优惠。
     */
    private BigDecimal deductionPrice;


    /**
     * 交易订单状态枚举
     * <p>
     * 描述实体的生命周期节点 (如：创建/锁单、已完成、失败/关闭)。
     * 这是一个值对象 (VO) 属性，用于描述实体的当前状态。
     */
    private TradeOrderStatusEnumVO tradeOrderStatusEnumVO;

}
package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易结算结果实体
 * <p>
 * 领域定义：
 * 订单支付结算完成后的“回执单”。
 * 包含了这笔交易的身份信息，以及结算后可能触发的拼团状态变更信息。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSettlementEntity {

    /** 内部订单ID (系统流转核心标识) */
    private String orderId;

    /** 外部交易单号 (支付渠道凭证) */
    private String outTradeNo;

    /** 用户ID */
    private String userId;

    /** 拼团团队ID */
    private String teamId;

    /** 活动ID */
    private Long activityId;

    /** 渠道 */
    private String source;

    /** 来源 */
    private String channel;
}
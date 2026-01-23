package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单请求实体
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundCommandEntity {
    /**用户ID */
    private String userId;

    /** 外部交易单号 */
    private String outTradeNo;

    /** 渠道 */
    private String source;

    /** 来源 */
    private String channel;
}

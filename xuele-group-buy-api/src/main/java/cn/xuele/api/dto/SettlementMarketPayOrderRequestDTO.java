package cn.xuele.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 结算请求对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 13:22
 */
@Data
public class SettlementMarketPayOrderRequestDTO {

    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 外部交易单号 */
    private String outTradeNo;
    /** 外部交易时间 */
    private LocalDateTime outTradeTime;

}

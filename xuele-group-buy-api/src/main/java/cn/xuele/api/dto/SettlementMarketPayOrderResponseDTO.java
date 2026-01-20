package cn.xuele.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算应答对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 13:23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettlementMarketPayOrderResponseDTO {

    /** 用户ID */
    private String userId;
    /** 拼单组队ID */
    private String teamId;
    /** 活动ID */
    private Long activityId;
    /** 外部交易单号 */
    private String outTradeNo;

}

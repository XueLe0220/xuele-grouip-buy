package cn.xuele.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营销拼团退单响应对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/04/19 16:33
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundMarketPayOrderResponseDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 退单行为状态码
     */
    private String code;

    /**
     * 退单行为状态信息
     */
    private String info;

}
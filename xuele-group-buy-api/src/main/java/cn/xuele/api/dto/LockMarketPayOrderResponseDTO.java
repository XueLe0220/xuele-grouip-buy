package cn.xuele.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 营销拼团锁单响应结果
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 23:41
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockMarketPayOrderResponseDTO {
    /** 预购订单ID */
    private String orderId;
    /** 原始价格 */
    private BigDecimal originalPrice;
    /** 折扣金额 */
    private BigDecimal deductionPrice;
    /** 应付金额 */
    private BigDecimal payableAmount;
    /** 交易订单状态 */
    private Integer tradeOrderStatus;
    /** 组队ID */
    private String teamId;

}
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

    /** 预购订单ID，系统内部生成的唯一交易单号 */
    private String orderId;

    /** 折扣金额，告诉前端用户省了多少钱 */
    private BigDecimal deductionPrice;

    /** 交易订单状态 (0-初始锁定)，通常用于前端判断是否跳转收银台 */
    private Integer tradeOrderStatus;

}
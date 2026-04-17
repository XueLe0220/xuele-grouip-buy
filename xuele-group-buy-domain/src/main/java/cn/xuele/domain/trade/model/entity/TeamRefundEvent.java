package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单事件
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/02/02 14:06
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamRefundEvent {

    /**
     * 退单类型
     */
    private String type;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 拼单组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 预购订单ID
     */
    private String orderId;
}

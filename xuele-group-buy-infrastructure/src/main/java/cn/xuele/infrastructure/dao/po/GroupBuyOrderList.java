package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 拼团订单明细 PO
 * <p>
 * 对应表：group_buy_order_list
 * 描述：记录用户的拼团契约，落库即代表“锁单成功”。
 *
 * @author XueLe
 * @since 2026/01/06
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderList {

    /** 自增主键 */
    private Long id;

    /** 用户ID */
    private String userId;

    /** 拼团团队ID */
    private String teamId;

    /** 系统内部订单ID */
    private String orderId;

    /** 活动ID */
    private Long activityId;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 商品ID */
    private String goodsId;

    /** 渠道标识 */
    private String source;

    /** 来源标识 */
    private String channel;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 折扣金额 */
    private BigDecimal deductionPrice;

    /** 订单状态：0-锁单成功(待支付)，1-支付成功 */
    private Integer status;

    /** 外部交易单号（用于幂等性去重） */
    private String outTradeNo;

    /** 业务唯一ID */
    private String bizId;

    /** 外部交易时间 */
    private LocalDateTime outTradeTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
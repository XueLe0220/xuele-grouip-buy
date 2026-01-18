package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户拼单明细 PO
 * <p>
 * 对应数据库表：group_buy_order_list
 * 作用：记录用户的交易契约，落库即代表“锁单成功”。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/06 23:37
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderList {

    /** 自增ID */
    private Long id;

    /** 用户ID (谁锁的单) */
    private String userId;

    /** 拼单组队ID (加入了哪个团) */
    private String teamId;

    /** 订单ID (系统内部唯一标识) */
    private String orderId;

    /** 活动ID */
    private Long activityId;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 商品ID */
    private String goodsId;

    /** 渠道 */
    private String source;

    /** 来源 */
    private String channel;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 折扣金额 */
    private BigDecimal deductionPrice;

    /**
     * 订单状态
     * 0-初始锁定 (锁单成功，待支付)
     * 1-消费完成 (支付成功)
     */
    private Integer status;

    /**
     * 外部交易单号 (面试核心：幂等性)
     * 作用：对接支付宝/微信时使用，确保同一笔订单无论用户点击多少次支付，都只扣款一次。
     */
    private String outTradeNo;

    /** 唯一业务ID */
    private String bizId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
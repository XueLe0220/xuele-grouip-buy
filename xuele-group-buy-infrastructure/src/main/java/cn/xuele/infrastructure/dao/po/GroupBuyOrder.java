package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 拼团组队聚合单 PO
 * <p>
 * 对应数据库表：group_buy_order
 * 作用：管理拼团的“坑位”资源，是防止超卖的核心表。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/06 23:36
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrder {

    /** 自增ID */
    private Long id;

    /**
     * 拼单组队ID (核心)
     * 所有参与该团的用户都关联此ID，锁单时的并发竞争资源点。
     */
    private String teamId;

    /** 活动ID */
    private Long activityId;

    /** 渠道 (如: s01) */
    private String source;

    /** 来源 (如: c01) */
    private String channel;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 折扣金额 */
    private BigDecimal deductionPrice;

    /** 支付价格 */
    private BigDecimal payPrice;

    /**
     * 目标数量
     * 例如：3人成团，此值为3。
     */
    private Integer targetCount;

    /**
     * 完成数量
     * 只有用户【支付成功】后，此值才会增加。
     */
    private Integer completeCount;

    /**
     * 锁单数量
     * 业务含义：已下单但【未支付】的占位人数。
     */
    private Integer lockCount;

    /**
     * 状态
     * 0-拼单中 (允许继续锁单)
     * 1-完成 (满员且支付成功)
     * 2-失败 (超时未满员)
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
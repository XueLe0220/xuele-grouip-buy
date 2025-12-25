package cn.xuele.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 拼团活动配置聚合对象 (Activity + Discount)
 * <p>
 * 聚合了"活动规则"与"折扣配置"，作为营销试算的核心规则载体。
 *
 * @author XueLe
 * @version 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivityDiscountVO {

    // --- 活动基础信息 ---
    private Long activityId;
    private String activityName;
    private String source;
    private String channel;
    private String goodsId;

    // --- 拼团限制规则 ---
    /** 拼团方式（0自动成团、1达成目标拼团） */
    private Integer groupType;
    /** 每人限购次数 */
    private Integer takeLimitCount;
    /** 成团目标人数 */
    private Integer target;
    /** 拼团有效时长（分钟） */
    private Integer validTime;

    // --- 状态与周期 ---
    /** 活动状态（0创建、1生效、2过期、3废弃） */
    private Integer status;
    /** 开始时间  */
    private LocalDateTime startTime;
    /** 结束时间  */
    private LocalDateTime endTime;

    // --- 人群门槛 ---
    /** 人群标签ID */
    private String tagId;
    /** 标签作用范围 */
    private String tagScope;

    /**
     * 强关联：折扣配置
     * 一个拼团活动必须绑定一个具体的折扣策略
     */
    private GroupBuyDiscount groupBuyDiscount;

    /**
     * 内部类：折扣配置详情
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupBuyDiscount {
        /** 折扣名称 */
        private String discountName;
        /** 折扣描述 */
        private String discountDesc;

        /** 折扣类型（0:base、1:tag） */
        private Integer discountType;

        /** * 营销计划 (核心策略标识)
         * ZJ:直减, MJ:满减, N:N元购
         */
        private String marketPlan;

        /** * 营销表达式 (核心计算参数)
         * 例: "100" (直减100), "0.8" (八折)
         */
        private String marketExpr;

        /** 专享人群标签 */
        private String tagId;
    }
}
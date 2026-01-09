package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 支付活动实体
 * <p>
 * 领域定义：
 * 在交易（Trade）上下文中，它代表了当前这笔交易依附的“活动规则”和“组队环境”。
 * 它不是单纯的活动配置，而是包含了“当前队伍(teamId)”上下文的活动实体。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayActivityEntity {

    /**
     * 拼单组队ID (关键标识)
     * <p>
     * 区分这笔交易是属于哪个“团”的。
     * 如果是开团（新团），此字段可能在业务处理过程中生成；如果是参团，必须传入。
     */
    private String teamId;

    /**
     * 活动ID (关键标识)
     * <p>
     * 对应 group_buy_activity.activity_id。
     * 所有的优惠规则、库存限制都挂载在这个ID下。
     */
    private Long activityId;

    /**
     * 活动名称
     * <p>
     * 用于订单快照记录，或者前端展示。
     */
    private String activityName;

    /**
     * 拼团开始时间
     * <p>
     * 业务校验点：当前时间必须 >= startTime，否则活动未开始，锁单应失败。
     * (已升级为 LocalDateTime)
     */
    private LocalDateTime startTime;

    /**
     * 拼团结束时间
     * <p>
     * 业务校验点：当前时间必须 <= endTime，否则活动已结束，锁单应失败。
     * (已升级为 LocalDateTime)
     */
    private LocalDateTime endTime;

    /**
     * 目标数量
     * <p>
     * 也就是“成团门槛”，例如 3人成团。
     * 对应 group_buy_activity.target。
     * 在锁单逻辑中，用于校验 (lockCount + completeCount) < targetCount。
     */
    private Integer targetCount;

}
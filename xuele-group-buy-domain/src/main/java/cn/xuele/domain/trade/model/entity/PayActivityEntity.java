package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付活动实体
 * <p>
 * 领域含义：交易上下文中的活动聚合实体。
 * 作用：封装当前交易依附的“活动规则”与“队伍上下文”（不仅是静态配置，还包含当前队伍信息）。
 *
 * @author XueLe
 * @since 2026/01/07
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayActivityEntity {

    /** 拼团团队ID (区分归属的拼团队伍) */
    private String teamId;

    /** 活动ID (关联 group_buy_activity) */
    private Long activityId;

    /** 活动名称 (用于快照记录或展示) */
    private String activityName;

    /** 活动开始时间 (用于校验活动是否开启) */
    private LocalDateTime startTime;

    /** 活动结束时间 (用于校验活动是否过期) */
    private LocalDateTime endTime;

    /** 成团目标数 (如3人团，核心校验参数) */
    private Integer targetCount;

    /** 拼团有效时长 (单位：分钟) */
    private Integer validTime;

}
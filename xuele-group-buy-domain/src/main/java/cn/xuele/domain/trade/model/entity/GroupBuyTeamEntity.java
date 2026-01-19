package cn.xuele.domain.trade.model.entity;

import cn.xuele.types.enums.GroupBuyTeamOrderVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 拼团组队详情实体
 * <p>
 * 领域定义：拼团活动中的“团”聚合根。
 * 作用：记录一个团的实时进度（锁单量、完成量）以及有效期。
 * 核心逻辑：Settlement Service 需要读取该实体，判断用户的支付是否触发了“拼团成功”。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamEntity {

    /** 拼单组队ID (主键) */
    private String teamId;

    /** 活动ID (关联配置) */
    private Long activityId;

    /** 目标数量 (成团门槛，如3人) */
    private Integer targetCount;

    /** 完成数量 (实际支付人数) */
    private Integer completeCount;

    /** 锁单数量 (占用坑位人数) */
    private Integer lockCount;

    /** 团状态 (拼单中/完成/失败) */
    private GroupBuyTeamOrderVO status;

    /** * 拼团有效开始时间
     * (通常是团长开团的时间)
     */
    private LocalDateTime validStartTime;

    /** * 拼团有效结束时间
     * (StartTime + 24小时，超过此时间未满员则FAIL)
     */
    private LocalDateTime validEndTime;

}
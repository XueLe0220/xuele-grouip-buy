package cn.xuele.domain.trade.model.entity;

import cn.xuele.types.enums.ActivityStatusEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 拼团活动实体对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivityEntity {

    /** 活动ID */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 折扣ID */
    private String discountId;

    /** 拼团方式（0:自动成团，1:达成目标拼团） */
    private Integer groupType;

    /** 每人限拼次数 */
    private Integer takeLimitCount;

    /** 成团目标人数 */
    private Integer target;

    /** 拼团有效期（分钟） */
    private Integer validTime;

    /** 活动状态 */
    private ActivityStatusEnumVO status;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 人群标签规则ID */
    private String tagId;

    /** 人群标签范围 */
    private String tagScope;

}
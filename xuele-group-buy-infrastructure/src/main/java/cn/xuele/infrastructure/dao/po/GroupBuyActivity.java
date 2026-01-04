package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 持久化对象：拼团活动表
 * <p>
 * 对应数据库表：group_buy_activity
 * 记录拼团活动的配置信息，包括时间、库存、参与门槛等。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 02:35
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivity {

    /** 自增ID */
    private Long id;

    /** 活动ID (业务主键) */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 折扣ID (注意：建议与 Discount 表统一类型) */
    private String discountId;

    /** 拼团方式（0:自动成团、1:达成目标拼团） */
    private Integer groupType;

    /** 拼团次数限制 */
    private Integer takeLimitCount;

    /** 拼团目标人数 */
    private Integer target;

    /** 拼团时长（分钟） */
    private Integer validTime;

    /** 活动状态（0:创建、1:生效、2:过期、3:废弃） */
    private Integer status;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 人群标签规则标识 */
    private String tagId;

    /** 人群标签规则范围 */
    private String tagScope;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
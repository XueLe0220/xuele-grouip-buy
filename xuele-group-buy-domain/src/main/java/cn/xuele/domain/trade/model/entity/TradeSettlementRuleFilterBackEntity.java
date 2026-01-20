package cn.xuele.domain.trade.model.entity;

import cn.xuele.domain.trade.model.valobj.NotifyConfigVO;
import cn.xuele.types.enums.GroupBuyTeamOrderVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 结算规则过滤结果实体 (BackEntity)
 * <p>
 * 领域定义：规则引擎执行完毕后的“输出报告”。
 * 作用：透传从数据库查询到的最新拼团信息，供 Service 层直接使用，避免二次查库。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSettlementRuleFilterBackEntity {

    /** 拼单组队ID (核心标识) */
    private String teamId;

    /** 活动ID */
    private Long activityId;

    /** 目标数量 (成团门槛) */
    private Integer targetCount;

    /** * 完成数量 (当前进度)
     * Service 层需基于此值进行 +1 操作。
     */
    private Integer completeCount;

    /** 锁单数量 (当前占用) */
    private Integer lockCount;

    /** 团状态 (拼单中/完成/失败) */
    private GroupBuyTeamOrderVO status;

    /** * 拼团有效开始时间
     * (已标准化为 LocalDateTime)
     */
    private LocalDateTime validStartTime;

    /** * 拼团有效结束时间
     * (已标准化为 LocalDateTime)
     */
    private LocalDateTime validEndTime;

    /** 回调配置 */
    private NotifyConfigVO notifyConfigVO;

}
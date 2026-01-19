package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易规则过滤指令实体
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:33
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLockRuleCommandEntity {

    /** 用户ID（谁发起交易） */
    private String userId;

    /** 活动ID（参与哪个活动） */
    private Long activityId;

}
package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易规则过滤反馈实体
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:33
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLockRuleFilterBackEntity {

    /** 用户已参与该活动的订单数量（用于限购规则校验） */
    private Integer userTakeOrderCount;

    /** 恢复组队库存缓存key */
    private String recoveryTeamStockKey;


}
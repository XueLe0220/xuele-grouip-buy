package cn.xuele.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 拼团进度值对象
 * <p>
 * 领域定义：
 * 用于描述某个拼团队伍（Team）当前的“拥挤程度”。
 * 它是做锁单决策的核心依据。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:17
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyProgressVO {

    /**
     * 目标数量
     * (例如：3人成团)
     */
    private Integer targetCount;

    /**
     * 完成数量
     * (已支付的人数)
     */
    private Integer completeCount;

    /**
     * 锁单数量
     * (已下单但未支付的占位人数)
     */
    private Integer lockCount;

}
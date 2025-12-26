package cn.xuele.domain.activity.service.discount;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

/**
 * 优惠计算策略服务接口
 * <p>
 * 架构层级：Domain Layer (领域层接口)
 * 设计模式：Strategy Pattern (策略模式) - 抽象策略角色
 * <p>
 * 职责：定义所有优惠算法（如直减、满减、折扣等）必须遵循的统一契约。
 * 不同的优惠类型将实现此接口，提供具体的计算逻辑。
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25 15:54
 */
public interface IDiscountCalculateService {

    /**
     * 计算优惠后的支付金额
     *
     * @param userId          用户ID
     * <p>作用：用于风控校验或特定用户的特殊逻辑（例如：新人专享价、黑名单用户不给优惠）。
     * 虽然纯数学计算不需要它，但在业务场景中它是必须的上下文。</p>
     *
     * @param originalPrice   商品原价
     * <p>作用：计算的基础金额。必须使用 BigDecimal 以保证货币计算精度。</p>
     *
     * @param groupBuyDiscount 拼团折扣配置对象
     * <p>作用：策略的"配置参数"。
     * 内部包含具体的规则表达式（如 "100-10" 或 "0.8"），策略实现类需解析此对象。</p>
     *
     * @return BigDecimal     <b>最终支付金额 (Pay Price)</b>
     * <p>注意：这里约定返回的是计算后的最终价格，而不是减去的金额。</p>
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);
}
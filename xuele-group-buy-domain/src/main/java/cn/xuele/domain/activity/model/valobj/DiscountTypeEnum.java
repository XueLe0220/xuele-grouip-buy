package cn.xuele.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 优惠类型枚举 (Discount Type)
 * <p>
 * 作用：区分优惠计算的"触发维度"。
 * 用于在策略模式中决定是走通用的金额计算，还是走特定的人群规则判定。
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25 16:00
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DiscountTypeEnum {

    /**
     * 0: 基础优惠 (Base Discount)
     * <p>含义：通用型优惠，仅依赖商品金额或数量进行计算。</p>
     * <p>场景：直减、满减、折扣等。</p>
     */
    BASE(0, "基础优惠"),

    /**
     * 1: 人群标签 (Crowd Tags)
     * <p>含义：限定型优惠，依赖用户画像标签进行过滤。</p>
     * <p>场景：新人专享、VIP特价、学生认证优惠等。</p>
     */
    TAG(1, "人群标签");

    private Integer code;
    private String info;

    /**
     * 根据 code 获取枚举对象
     * 建议：相比 switch，这种写法在枚举增多时更容易维护，
     * 但对于只有两个值的枚举，你的 switch 写法性能是最好的。
     */
    public static DiscountTypeEnum get(Integer code) {
        if (code == null) return null;
        switch (code) {
            case 0:
                return BASE;
            case 1:
                return TAG;
            default:
                throw new IllegalArgumentException("Unknown DiscountTypeEnum code: " + code);
        }
    }

}
package cn.xuele.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团试算平衡实体（试算服务标准出参）
 * <p>
 * 描述：承载营销规则试算后的最终结果。不仅包含金额计算，还包含活动的元数据和准入状态。
 * 前端将直接依据此对象的数据渲染“购买按钮”的状态（如：显示倒计时、置灰不可买等）。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceEntity {

    /** 商品ID (透传返回，方便前端映射) */
    private String goodsId;

    /** 商品名称 (展示用) */
    private String goodsName;

    /** * 原始价格
     * 作用：作为价格计算的基准线，前端展示时的“划线价”。
     */
    private BigDecimal originalPrice;

    /** * 折扣价格 (即：减免了多少钱)
     * 作用：
     * 1. 这里的含义是 deduction (减去的部分)，不是最终支付价。
     * 2. 最终支付价 = originalPrice - deductionPrice。
     */
    private BigDecimal deductionPrice;

    /**
     * 支付价格
     */
    private BigDecimal payPrice;

    /** * 拼团目标数量
     * 作用：用于前端展示进度条或文案（例：“还差2人成团”）。
     */
    private Integer targetCount;

    /** * 拼团开始时间
     * 作用：如果当前时间 < startTime，前端需要展示“预热/倒计时”状态。
     */
    private LocalDateTime startTime;

    /** * 拼团结束时间
     * 作用：用于前端倒计时计算，超过此时间则活动结束。
     */
    private LocalDateTime endTime;

    /** * 是否可见拼团 (IsVisible)
     * 作用：【显隐控制】
     * 场景：如果活动配置了“仅新人可见”，老用户试算时此字段为 false，前端整个活动卡片应隐藏。
     */
    private Boolean isVisible;

    /** * 是否可参与进团 (IsEnable)
     * 作用：【交互控制】
     * 场景：虽然活动可见（isVisible=true），但如果“库存不足”或“时间未到”，
     * 此字段为 false，前端按钮应置灰（Disable）并提示原因。
     */
    private Boolean isEnable;
}
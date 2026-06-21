package cn.xuele.domain.activity.model.entity;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团试算平衡实体
 * <p>
 * 承载营销规则试算后的最终结果，包含价格计算详情及活动准入状态。
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

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原始价格（划线价） */
    private BigDecimal originalPrice;

    /** 优惠金额（抵扣金额，即：原价-应付金额） */
    private BigDecimal deductionPrice;

    /** 最终应付金额 */
    private BigDecimal payableAmount;

    /** 拼团目标成团数量 */
    private Integer targetCount;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 是否可见（控制前端活动卡片的显隐） */
    private Boolean isVisible;

    /** 是否可参与（控制前端按钮是否置灰/可点击） */
    private Boolean isEnable;

    /** 关联的拼团活动配置信息 */
    private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
}
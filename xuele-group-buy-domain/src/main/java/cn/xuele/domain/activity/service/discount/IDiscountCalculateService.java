package cn.xuele.domain.activity.service.discount;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

/**
 * 优惠计算策略服务接口
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/25 15:54
 */
public interface IDiscountCalculateService {

    /**
     * 计算优惠后的支付金额
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount, boolean isUsable);
}
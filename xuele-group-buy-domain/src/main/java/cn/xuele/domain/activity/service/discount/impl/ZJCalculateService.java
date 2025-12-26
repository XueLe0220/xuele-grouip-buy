package cn.xuele.domain.activity.service.discount.impl;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 直减优惠策略 (Direct Deduction Strategy)
 * <p>
 * 规则：原价 - 优惠金额
 * 表达式：x (例如 "10" 代表减去 10 元)
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25
 */
@Slf4j
@Service("ZJ")
public class ZJCalculateService extends AbstractDiscountCalculateService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        // 1. 获取直减金额
        String marketExpr = groupBuyDiscount.getMarketExpr();
        BigDecimal deductionAmount = new BigDecimal(marketExpr.trim());

        // 2. 计算：原价 - 减免额
        BigDecimal payPrice = originalPrice.subtract(deductionAmount);

        // 3. 兜底校验：最低支付 0.01 元
        if (payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return payPrice;
    }
}
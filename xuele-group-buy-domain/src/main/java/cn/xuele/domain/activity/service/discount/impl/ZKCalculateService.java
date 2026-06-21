package cn.xuele.domain.activity.service.discount.impl;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣优惠策略 (Discount Rate Strategy)
 * <p>
 * 规则：原价 * 折扣率
 * 表达式：x (例如 "0.8" 代表 8折，"0.95" 代表 95折)
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25
 */
@Slf4j
@Service("ZK")
public class ZKCalculateService extends AbstractDiscountCalculateService {

    @Override
    public BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        // 1. 获取折扣率 (例如 0.8)
        String marketExpr = groupBuyDiscount.getMarketExpr();
        BigDecimal discountRate = new BigDecimal(marketExpr.trim());


        // 2. 计算：原价 * 折扣率
        BigDecimal payableAmount = originalPrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);

        // 3. 兜底校验：最低支付 0.01 元
        if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return payableAmount;
    }
}
package cn.xuele.domain.activity.service.discount.impl;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 满减计算策略 (MJ - Man Jian)
 * <p>
 * 规则表达式格式：x,y (满x减y)
 * 例如：100,10 -> 满100元减10元
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/25 16:18
 */
@Slf4j
@Service("MJ")
public class MJCalculateService extends AbstractDiscountCalculateService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice,
                                     GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        String marketExpr = groupBuyDiscount.getMarketExpr();

        // 1. 健壮性校验：防止空指针或格式错误
        if (marketExpr == null || !marketExpr.contains(",")) {
            log.error("MJ策略表达式格式错误: {}", marketExpr);
            // 遇到配置错误，为了不阻断交易，通常选择不优惠（返回原价）或抛出自定义业务异常
            return originalPrice;
        }

        // 2. 解析表达式 (100, 10)
        String[] split = marketExpr.split(",");
        // 门槛金额 (Threshold)
        BigDecimal thresholdAmount = new BigDecimal(split[0].trim());
        // 优惠金额 (Deduction)
        BigDecimal discountAmount = new BigDecimal(split[1].trim());

        // 3. 判断是否满足门槛 (原价 < 门槛，不满足)
        if (originalPrice.compareTo(thresholdAmount) < 0) {
            return originalPrice;
        }

        // 4. 计算优惠后金额
        BigDecimal payableAmount = originalPrice.subtract(discountAmount);

        // 5. 兜底策略：防止金额为负或0，最低支付0.01
        if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return payableAmount;
    }
}
package cn.xuele.domain.activity.service.discount.impl;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * N元购优惠策略 (Fixed Price Strategy)
 * <p>
 * 规则：无论原价多少，最终支付金额固定为 N 元。
 * 表达式：N (例如 "9.9" 代表最终只需付 9.9 元)
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25
 */
@Slf4j
@Service("N")
public class NCalculateService extends AbstractDiscountCalculateService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());
        // 1. 获取规则表达式 (例如 "9.9")
        String marketExpr = groupBuyDiscount.getMarketExpr();

        // 2. 直接将其转换为 BigDecimal 作为最终价格
        BigDecimal returnPrice = new BigDecimal(marketExpr.trim());

        // 3. 即使是N元购，也建议做一个最小支付金额校验，防止运营配成 0 或负数
        if (returnPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return returnPrice;
    }
}
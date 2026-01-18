package cn.xuele.domain.activity.service.discount;

import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.DiscountTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 优惠计算抽象类 (Abstract Strategy)
 * <p>
 * 设计模式：Template Method Pattern (模板方法模式)
 * 职责：
 * 1. 编排标准的计算流程：用户资格校验 -> 具体的金额计算。
 * 2. 将通用的"人群过滤"逻辑上浮到父类，子类只需关注算法本身。
 *
 * @author XueLe (肖金城)
 * @version 1.0.0
 * @since 2025/12/25 15:55
 */
@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService {

    protected IActivityRepository repository;

    /**
     * 模板方法：定义计算骨架
     */
    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice,
                                GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {

        // 1. 获取优惠类型
        DiscountTypeEnum discountType = groupBuyDiscount.getDiscountType();

        // 2. 人群标签过滤 (仅针对 TAG 类型优惠)
        if (DiscountTypeEnum.TAG.equals(discountType)) {
            // tagId 就是我们在大数据平台圈选的人群包ID
            boolean isCrowdRange = filterTagId(groupBuyDiscount.getTagId(), userId);

            // 如果不在人群范围内，不予优惠，直接返回原价
            if (!isCrowdRange) {
                log.info("折扣优惠计算拦截，用户不再优惠人群标签范围内 userId:{}", userId);
                return originalPrice;
            }
        }

        // 3. 具体的优惠计算 (交给子类实现 - 直减/满减)
        return doCalculate(originalPrice, groupBuyDiscount);
    }

    /**
     * 抽象方法：具体的优惠算法
     */
    protected abstract BigDecimal doCalculate(BigDecimal originalPrice,
                                              GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

    /**
     * 人群标签校验
     * * @param userId 用户ID
     *
     * @param tagId 人群标签ID (例如：ABCD_001 代表"高净值用户")
     * @return boolean true-在人群内(或无限制) / false-不在人群内
     */
    private boolean filterTagId(String tagId, String userId) {
        return repository.isUserInTag(tagId, userId);
    }
}
package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import cn.xuele.domain.activity.service.discount.IDiscountCalculateService;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.domain.activity.service.trial.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import cn.xuele.domain.activity.service.trial.thread.QuerySkuVOThreadTask;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 营销节点 (Market Node)
 * <p>
 * 职责：
 * 1. 异步并行加载数据 (活动规则 + 商品信息)。
 * 2. 路由具体的优惠策略进行价格计算。
 * 3. 将计算结果(原价、抵扣价、最终价)写入上下文。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:42
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketNode extends AbstractGroupBuyMarketSupport {

    private final ThreadPoolExecutor threadPoolExecutor;
    private final TagNode tagNode;
    private final ErrorNode errorNode;
    // Key: BeanName (例如 "ZJ", "MJ"), Value: Bean实例
    private final Map<String, IDiscountCalculateService> discountCalculateServiceMap;

    @Override
    protected void multiThread(MarketProductEntity requestParameter,
                               DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException {

        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask =
                new QueryGroupBuyActivityDiscountVOThreadTask(
                        requestParameter.getGoodsId(),
                        repository
                );

        QuerySkuVOThreadTask querySkuVOThreadTask = new QuerySkuVOThreadTask(
                requestParameter.getGoodsId(),
                repository
        );

        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask =
                new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOThreadTask);

        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        GroupBuyActivityDiscountVO activityDiscountVO = groupBuyActivityDiscountVOFutureTask.get();
        SkuVO skuVO = skuVOFutureTask.get();

        dynamicContext.setSkuVO(skuVO);
        dynamicContext.setGroupBuyActivityDiscountVO(activityDiscountVO);

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据完成", requestParameter.getUserId());
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter,
                                      DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(),
                JSON.toJSONString(requestParameter));

        // 1. 从上下文获取数据
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        if (null == groupBuyActivityDiscountVO) {
            return router(requestParameter, dynamicContext);
        }
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = groupBuyActivityDiscountVO.getGroupBuyDiscount();
        SkuVO skuVO = dynamicContext.getSkuVO();
        if (null == skuVO) {
            return router(requestParameter, dynamicContext);
        }

        // 2. 策略路由：获取具体的计算服务 (ZJ, MJ, N, ZK)
        String marketPlan = groupBuyDiscount.getMarketPlan();
        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(marketPlan);

        if (null == discountCalculateService) {
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", marketPlan, JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
        }

        // 3. 执行核心计算
        BigDecimal payPrice = discountCalculateService.calculate(requestParameter.getUserId(),
                skuVO.getOriginalPrice(), groupBuyDiscount);

        // 4. 计算优惠减免金额 (Deduction Price)
        // 减免额 = 原价 - 最终支付价
        BigDecimal deductionPrice = skuVO.getOriginalPrice().subtract(payPrice);

        // 5. 将结果回填到 Context，供 EndNode 组装最终结果
        dynamicContext.setDeductionPrice(deductionPrice); // 优惠了多少
        dynamicContext.setPayPrice(payPrice);             // 最终付多少

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        // 不存在配置的拼团活动，走异常节点
        if (null == dynamicContext.getGroupBuyActivityDiscountVO() || null == dynamicContext.getSkuVO() || null == dynamicContext.getDeductionPrice()) {
            return errorNode;
        }
        return tagNode;
    }
}
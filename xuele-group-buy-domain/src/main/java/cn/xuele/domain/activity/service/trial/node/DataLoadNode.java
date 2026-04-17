package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SCSkuActivityVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 核心数据加载节点
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/02/02 09:32
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataLoadNode extends AbstractGroupBuyMarketSupport {

    private final SwitchNode switchNode;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void multiThread(MarketProductEntity requestParameter,
                               DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException {

        String goodsId = requestParameter.getGoodsId();
        String source = requestParameter.getSource();
        String channel = requestParameter.getChannel();

        CompletableFuture<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(goodsId, source, channel);
            if (null == scSkuActivityVO) {
                log.error("该商品无关联活动");
                return null;
            }
            return repository.queryGroupBuyActivityDiscountVO(scSkuActivityVO.getActivityId());
        },threadPoolExecutor);

        CompletableFuture<SkuVO> skuVOCompletableFuture = CompletableFuture.supplyAsync(() -> repository.querySkuByGoodsId(goodsId),threadPoolExecutor);

        CompletableFuture.allOf(groupBuyActivityDiscountVOCompletableFuture, skuVOCompletableFuture).get();

        GroupBuyActivityDiscountVO activityDiscountVO = groupBuyActivityDiscountVOCompletableFuture.join();
        SkuVO skuVO = skuVOCompletableFuture.join();


        dynamicContext.setSkuVO(skuVO);
        dynamicContext.setGroupBuyActivityDiscountVO(activityDiscountVO);

        log.info("拼团商品查询试算服务-DataLoadNode userId:{} 异步线程加载数据完成", requestParameter.getUserId());
    }

    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter,
                                         DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return switchNode;
    }
}

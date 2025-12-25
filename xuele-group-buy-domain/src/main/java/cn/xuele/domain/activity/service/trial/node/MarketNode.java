package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.domain.activity.service.trial.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import cn.xuele.domain.activity.service.trial.thread.QuerySkuVOThreadTask;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TODO: 类描述
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
    private final EndNode endNode;


    /**
     * 异步并行加载数据 (钩子方法)
     * <p>
     * 核心逻辑：
     * 1. 组装任务：创建两个独立的查询任务。
     * 2. 并行执行：将任务提交给线程池，利用 IO 等待时间。
     * 3. 结果聚合：阻塞等待所有任务完成，并将结果填充到 DynamicContext。
     */
    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException {

        // 1. 创建异步任务实例
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(
                requestParameter.getSource(),
                requestParameter.getChannel(),
                repository
        );

        QuerySkuVOThreadTask querySkuVOThreadTask = new QuerySkuVOThreadTask(
                requestParameter.getGoodsId(),
                repository
        );

        // 2. 创建 FutureTask 包装器
        // FutureTask 实现了 RunnableFuture 接口，既可以被线程执行，又可以获取返回值
        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOThreadTask);

        // 3. 提交任务到线程池 (此时两个查询开始并行执行)
        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        // 4. 阻塞获取结果 (Barrier/Join)
        // .get() 会阻塞当前线程，直到对应的异步任务执行完毕
        // 只有当两个任务都拿到结果后，才会继续往下执行
        GroupBuyActivityDiscountVO activityDiscountVO = groupBuyActivityDiscountVOFutureTask.get();
        SkuVO skuVO = skuVOFutureTask.get();

        // 5. 将结果装载到上下文 (DynamicContext)
        // 供后续的 doApply 方法直接使用
        dynamicContext.setSkuVO(skuVO);
        dynamicContext.setGroupBuyActivityDiscountVO(activityDiscountVO);

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // todo  拼团优惠试算

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return endNode;
    }
}

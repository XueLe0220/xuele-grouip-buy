package cn.xuele.domain.activity.service.trial;

import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.AbstractMultiThreadStrategyRouter;

import jakarta.annotation.Resource;
import java.util.concurrent.ExecutionException;

/**
 * 拼团市场业务通用支持节点
 * <p>
 * 职责描述：
 * 1. 【泛型锚定】：绑定业务类型 MarketProductEntity, DynamicContext, TrialBalanceEntity。
 * 2. 【默认钩子】：提供 multiThread 的默认空实现。只有真正需要查库的节点（如 SwitchNode）才需要重写它。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:28
 */
public abstract class AbstractGroupBuyMarketSupport extends AbstractMultiThreadStrategyRouter<MarketProductEntity,
        DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    protected long timeout = 500;
    @Resource
    protected IActivityRepository repository;

    /**
     * 默认的多线程预热方法（空实现）
     * <p>
     * 设计意图：
     * 绝大多数节点（如 RootNode, EndNode）不需要并行加载数据。
     * 为了避免每个子类都去写一个空的 override，这里统一由父类“兜底”。
     * 需要查库的节点（如 SwitchNode）请自行 Override 此方法。
     */
    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException {
        // 默认什么都不做
    }
}
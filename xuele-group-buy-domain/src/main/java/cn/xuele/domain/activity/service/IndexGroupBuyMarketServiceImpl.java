package cn.xuele.domain.activity.service;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 首页营销试算服务实现类
 * <p>
 * 实现逻辑：
 * 1. 编排策略模式：通过工厂获取责任链/规则树的根节点。
 * 2. 初始化上下文：构建用于承载动态配置的 Context 对象。
 * 3. 执行引擎：触发规则树的递归调用，完成从规则校验到价格计算的全流程。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 12:57
 */
@Service
@RequiredArgsConstructor
public class IndexGroupBuyMarketServiceImpl implements IIndexGroupBuyMarketService {

    /** 策略工厂：用于获取规则树执行入口 */
    private final DefaultActivityStrategyFactory defaultActivityStrategyFactory;

    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        // 1. 获取策略执行句柄（通常指向规则树的根节点 RootNode）
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler =
                defaultActivityStrategyFactory.strategyHandler();

        // 2. 初始化动态上下文（用于在节点流转间传递活动配置、中间计算结果等数据）
        DefaultActivityStrategyFactory.DynamicContext dynamicContext = new DefaultActivityStrategyFactory.DynamicContext();

        // 3. 执行策略逻辑并返回试算结果
        return strategyHandler.apply(marketProductEntity, dynamicContext);
    }
}
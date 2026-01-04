package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 渠道/活动规则分支节点 (Switch Node)
 * <p>
 * 职责描述：
 * 1. 【服务降级】：(未来实现) 检查动态配置中心开关，如果系统负载过高，直接熔断，返回兜底方案。
 * 2. 【人群切量】：(未来实现) 根据 userId 进行 Hash 取模，实现 A/B Test 或灰度发布流量控制。
 * 3. 【路由分发】：决定请求是进入核心营销计算，还是走其他轻量级链路。
 * <p>
 * 注意：本节点应当保持"轻量级"，尽量避免耗时的数据库查询，确保流量控制的快速响应。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:42
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SwitchNode extends AbstractGroupBuyMarketSupport {

    private final MarketNode marketNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 1. TODO: 检查服务降级开关 (SwitchConfig)
        // if (downgradeSwitch.isOn()) { return defaultResult; }

        // 2. TODO: 人群切量逻辑
        // if (userId.hashCode() % 100 < 50) { ... }

        // 目前阶段：直接放行，流转到 MarketNode 进行业务处理
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return marketNode;
    }

}
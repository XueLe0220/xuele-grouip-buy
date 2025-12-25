package cn.xuele.types.design.framework.tree;

import lombok.Getter;
import lombok.Setter;

/**
 * 策略路由抽象类
 * <p>
 * 核心功能：
 * 1. 编排策略执行流程（模板方法模式）：先获取策略 -> 只有策略存在时才执行 -> 否则执行默认策略。
 * 2. 提供默认兜底策略，防止空指针。
 *
 * @author XueLe
 * @since 2025/12/24
 */
public abstract class AbstractStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    /**
     * 默认策略处理器 (兜底逻辑)
     */
    @Getter
    @Setter
    protected StrategyHandler<T, D, R> defaultStrategyHandler = StrategyHandler.DEFAULT;

    /**
     * 执行路由逻辑 (Template Method)
     * <p>
     * 这里的逻辑是固定的：先找策略，找到了就跑，找不到就跑兜底。
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 执行结果
     * @throws Exception 异常
     */
    public R router(T requestParameter, D dynamicContext) throws Exception {
        // 1. 调用实现类的 get 方法，查找是否有匹配的策略
        StrategyHandler<T, D, R> strategyHandler = get(requestParameter, dynamicContext);

        // 2. 如果找到了策略，执行该策略
        if (null != strategyHandler) {
            return strategyHandler.apply(requestParameter, dynamicContext);
        }

        // 3. 如果没找到，执行默认兜底策略
        return defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }
}
package cn.xuele.types.design.framework.tree;

/**
 * 策略映射器接口
 * <p>
 * 用于根据入参获取具体的策略实现类。
 *
 * @author XueLe
 * @since 2025/12/24
 */
public interface StrategyMapper<T, D, R> {

    /**
     * 获取策略处理器
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 匹配到的策略处理器，如果未匹配则可能返回 null
     */
    StrategyHandler<T, D, R> get(T requestParameter, D dynamicContext) throws Exception;
}
package cn.xuele.types.design.framework.tree;

/**
 * 策略处理器接口
 * <p>
 * 定义通用的策略执行行为，所有具体的决策节点（如：白名单校验、库存校验）都需实现此接口。
 *
 * @param <T> requestParameter  标准请求入参（如：决策请求对象）
 * @param <D> dynamicContext    动态上下文（如：决策过程中产生的中间数据）
 * @param <R> returnType        返回结果类型（如：决策结果 true/false 或 具体值）
 *
 * @author XueLe
 * @since 2025/12/24
 */
public interface StrategyHandler<T, D, R> {

    /**
     * 默认策略：不做任何处理，直接返回 null
     */
    StrategyHandler DEFAULT = (T, D) -> null;

    /**
     * 执行具体策略逻辑
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 策略执行结果
     * @throws Exception 执行异常
     */
    R apply(T requestParameter, D dynamicContext) throws Exception;
}
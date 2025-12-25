package cn.xuele.types.design.framework.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 抽象多线程策略路由器 (模板方法模式应用)
 * <p>
 * 核心职责：
 * 1. 提供标准的策略路由分发能力 {@link #router}。
 * 2. 定义"异步预加载 -> 同步业务执行"的标准执行模板 {@link #apply}。
 *
 * @param <T> 请求参数类型 (Request Parameter)
 * @param <D> 动态上下文类型 (Dynamic Context)
 * @param <R> 返回结果类型 (Result)
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 16:51
 */
public abstract class AbstractMultiThreadStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    /**
     * 默认策略处理器
     * 当路由逻辑 {@link #get(Object, Object)} 无法匹配到具体策略时，兜底执行此处理器。
     */
    @Getter
    @Setter
    protected StrategyHandler<T, D, R> defaultStrategyHandler = StrategyHandler.DEFAULT;

    /**
     * 执行策略路由 (标准分发入口)
     * <p>
     * 根据入参获取对应的策略处理器，如果未找到则执行默认策略。
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    public R router(T requestParameter, D dynamicContext) throws Exception {
        // 1. 尝试获取具体的策略处理器 (由子类实现 get 方法逻辑)
        StrategyHandler<T, D, R> strategyHandler = get(requestParameter, dynamicContext);

        // 2. 如果找到策略，则执行
        if (null != strategyHandler) {
            return strategyHandler.apply(requestParameter, dynamicContext);
        }

        // 3. 未找到策略，执行兜底逻辑
        return defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }

    /**
     * 模板执行方法 (Template Method)
     * <p>
     * 编排了 "多线程预处理" 与 "核心业务执行" 的顺序。
     * 所有继承此类的节点，在被调用 apply 时，都会自动先触发 multiThread()。
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 执行结果
     * @throws Exception 执行异常
     */
    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        // 1. 触发多线程/异步处理钩子 (例如：并行加载数据、异步埋点等)
        multiThread(requestParameter, dynamicContext);

        // 2. 执行核心业务逻辑
        return doApply(requestParameter, dynamicContext);
    }

    /**
     * 执行核心业务逻辑 (由子类实现)
     * <p>
     * 此方法在 {@link #multiThread(T requestParameter, D dynamicContext)} 之后被调用。
     * 此时应当假设必要的异步数据已经提交或准备就绪。
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 业务结果
     */
    protected abstract R doApply(T requestParameter, D dynamicContext) throws Exception;

    /**
     * 多线程/异步处理钩子 (由子类实现)
     * <p>
     * 典型场景：
     * 1. 开启 CompletableFuture 并行查询数据库/RPC。
     * 2. 将 Future 对象放入 dynamicContext 中。
     * 3. 在 doApply 中通过 Future.get() 获取数据。
     */
    protected abstract void multiThread(T requestParameter, D dynamicContext) throws ExecutionException, InterruptedException, TimeoutException;

}
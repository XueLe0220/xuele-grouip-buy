package cn.xuele.types.design.framework.link.handler;

/**
 * 责任链模式 - 逻辑处理器标准接口
 * <p>
 * 定义了责任链中每一个节点的行为规范。
 * 所有的业务逻辑节点（如：库存校验、风控校验）都必须实现此接口。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/09 16:10
 */
public interface ILogicHandler<T, D, R> {

    /**
     * 获取下一个处理器（默认实现）
     * <p>
     * 通常由抽象基类重写以返回真实的下一个节点。
     * 接口层提供默认实现返回 null，作为链条结束的兜底。
     *
     * @return 下一个处理器节点的返回结果，默认 null
     */
    default R next(T requestParameter, D DynamicContext){
        return null;
    }

    /**
     * 核心业务逻辑执行方法
     * <p>
     * 具体的业务规则（如校验、计算）在此方法中实现。
     *
     * @param requestParameter 请求入参
     * @param dynamicContext   动态上下文
     * @return 执行结果
     * @throws Exception 允许抛出受检异常，由框架层统一捕获处理
     */
    R apply(T requestParameter, D dynamicContext) throws Exception;
}
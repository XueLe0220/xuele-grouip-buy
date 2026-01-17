package cn.xuele.types.design.framework.link.chain;

import cn.xuele.types.design.framework.link.handler.ILogicHandler;

/**
 * 责任链业务容器（核心执行引擎）
 * <p>
 * 该类是责任链模式的组装器和执行器。
 * 1. 继承自 {@link LinkedList}：赋予了它存储多个逻辑处理器的能力。
 * 2. 实现 {@link ILogicHandler}：赋予了它对外表现为一个独立节点的能力（组合模式）。
 * <p>
 * 外部调用者只需要调用本类的 {@link #apply} 方法，它就会自动在内部调度所有注册的子节点。
 *
 * @param <T> 请求入参类型
 * @param <D> 动态上下文类型
 * @param <R> 返回结果类型
 *
 * @author XueLe
 * @version 1.0.1 (Optimized)
 * @since 2026/01/16 16:10
 */
public class BusinessLinkedList<T, D, R> extends LinkedList<ILogicHandler<T, D, R>> implements ILogicHandler<T, D, R> {

    /**
     * 构造函数
     *
     * @param name 责任链名称（用于日志追踪或业务区分）
     */
    public BusinessLinkedList(String name) {
        super(name);
    }

    /**
     * 核心执行逻辑：链式调用
     * <p>
     * 顺序遍历链表中的所有处理器节点。
     * 采用<b>"拦截器模式"</b>策略：
     * 1. 如果某个节点返回了非空结果（!null），视为处理完成或被拦截，立即终止链条并返回该结果。
     * 2. 如果某个节点返回 null，视为通行（Pass），继续执行下一个节点。
     * 3. 如果所有节点都遍历完毕且都返回 null，则最终返回 null。
     *
     * @param requestParameter 请求参数
     * @param dynamicContext   动态上下文
     * @return 链条执行结果
     * @throws Exception 业务异常或系统异常
     */
    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {

        for (Node<ILogicHandler<T, D, R>> current = this.first; current != null; current = current.next) {

            // 获取当前节点的业务处理器
            ILogicHandler<T, D, R> handler = current.item;

            // 执行业务逻辑
            R result = handler.apply(requestParameter, dynamicContext);

            // 【决策点】
            // 如果结果不为空，说明当前节点拦截了请求（例如：风控拒绝、库存扣减失败、或成功生成了订单ID）
            // 此时不需要继续往后走，直接返回结果。
            if (result != null) {
                return result;
            }

            // 如果 result 为 null，循环继续，current 指向 next
        }

        // 链条跑完了，没有任何节点拦截/产生结果，返回兜底值
        return null;
    }
}
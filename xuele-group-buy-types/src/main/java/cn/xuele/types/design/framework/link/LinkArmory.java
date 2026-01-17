package cn.xuele.types.design.framework.link;

import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;

/**
 * 链路装配器 (Link Armory)
 * <p>
 * 核心作用：充当责任链的"工厂"或"建造者"。
 * 它将一组离散的逻辑处理器 {@link ILogicHandler} 封装并按照顺序装配成一条可执行的业务链路 {@link BusinessLinkedList}。
 * 对外屏蔽链表的构建细节，提供开箱即用的链路对象。
 *
 * @param <T> 请求入参类型 (Request)
 * @param <D> 领域上下文类型 (Domain Context)
 * @param <R> 响应结果类型 (Response)
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 11:41
 */
public class LinkArmory<T, D, R> {

    /**
     * 组装完成的业务逻辑链路
     */
    private final BusinessLinkedList<T, D, R> logicLink;

    /**
     * 构造并装配链路
     *
     * @param linkName      链路名称（用于日志追踪或监控）
     * @param logicHandlers 变长参数，按顺序传入需要执行的逻辑处理器
     */
    @SafeVarargs
    public LinkArmory(String linkName, ILogicHandler<T, D, R>... logicHandlers) {
        // 初始化链表
        logicLink = new BusinessLinkedList<>(linkName);

        // 防御性编程：防止传入 null 导致空指针
        if (logicHandlers != null && logicHandlers.length > 0) {
            for (ILogicHandler<T, D, R> logicHandler : logicHandlers) {
                logicLink.add(logicHandler);
            }
        }
    }

    /**
     * 获取装配好的业务链路
     *
     * @return 完整的责任链对象
     */
    public BusinessLinkedList<T, D, R> getLogicLink() {
        return logicLink;
    }
}
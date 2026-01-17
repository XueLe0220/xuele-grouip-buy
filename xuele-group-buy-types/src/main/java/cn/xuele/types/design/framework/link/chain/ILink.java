package cn.xuele.types.design.framework.link.chain;

/**
 * 责任链结构接口
 * <p>
 * 定义了责任链容器的基本操作标准。
 * 与 JDK 标准 List 相比，它更轻量，且专注于责任链节点的编排。
 *
 * @param <E> 链条中存储的元素类型（通常是 ILogicHandler 的实现）
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/09 16:12
 */
public interface ILink<E> {

    /**
     * 添加元素到链尾
     * @param e 元素
     * @return 是否添加成功
     */
    boolean add(E e);

    /**
     * 添加元素到链头
     * @param e 元素
     * @return 是否添加成功
     */
    boolean addFirst(E e);

    /**
     * 添加元素到链尾（显式）
     * @param e 元素
     * @return 是否添加成功
     */
    boolean addLast(E e);

    /**
     * 移除指定元素
     * @param o 要移除的对象
     * @return 是否移除成功
     */
    boolean remove(Object o);

    /**
     * 获取指定索引的元素
     * @param index 索引
     * @return 元素
     */
    E get(int index);

    /**
     * 打印链表结构
     * <p>
     * 主要用于调试，观察链条顺序。
     */
    void printLinkList();
}
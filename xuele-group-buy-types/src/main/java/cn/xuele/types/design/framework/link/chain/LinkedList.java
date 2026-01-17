package cn.xuele.types.design.framework.link.chain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 责任链专用链表容器
 * <p>
 * 一个基于双向链表实现的简易容器。
 * 核心目的：为了在责任链模式中，能够完全掌控节点的 prev/next 指针，
 * 从而实现比 java.util.LinkedList 更灵活的链式操作（如：手动调整节点顺序、获取节点引用等）。
 *
 * @param <E> 存储的元素类型（通常是 LogicHandler）
 * @author XueLe
 * @version 1.0.1
 * @since 2026/01/09 16:13
 */
@RequiredArgsConstructor
public class LinkedList<E> implements ILink<E> {

    /**
     * 责任链名称（用于区分不同的业务链，如：下单核销链、退款链）
     */
    @Getter
    private final String name;

    /**
     * 链表当前节点数量
     */
    transient int size = 0;

    /**
     * 头节点引用
     */
    transient Node<E> first;

    /**
     * 尾节点引用
     */
    transient Node<E> last;

    /**
     * 头插法：将元素链接到链表头部
     *
     * @param e 要插入的元素
     */
    void linkFirst(E e) {
        final Node<E> f = first;
        // 新节点：prev=null, item=e, next=f(旧头)
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;

        if (f == null)
            // 如果链表原本为空，新节点既是头也是尾
            last = newNode;
        else
            // 否则，旧头的 prev 指向新节点
            f.prev = newNode;
        size++;
    }

    /**
     * 尾插法：将元素链接到链表尾部
     *
     * @param e 要插入的元素
     */
    void linkLast(E e) {
        final Node<E> l = last;
        // 新节点：prev=l(旧尾), item=e, next=null
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;

        if (l == null) {
            // 如果链表原本为空，新节点既是头也是尾
            first = newNode;
        } else {
            // 否则，旧尾的 next 指向新节点
            l.next = newNode;
        }
        size++;
    }

    @Override
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean addFirst(E e) {
        linkFirst(e);
        return true;
    }

    @Override
    public boolean addLast(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 核心移除逻辑：断开指定节点的连接
     *
     * @param x 要移除的非空节点
     * @return 被移除元素的具体值
     */
    E unlink(Node<E> x) {
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (prev == null) {
            // 如果是头节点，first 指向它的下一个
            first = next;
        } else {
            // 否则，前驱的 next 跳过当前节点，指向当前节点的 next
            prev.next = next;
            x.prev = null; // 帮助 GC
        }

        if (next == null) {
            // 如果是尾节点，last 指向它的前一个
            last = prev;
        } else {
            // 否则，后继的 prev 跳过当前节点，指向当前节点的 prev
            next.prev = prev;
            x.next = null; // 帮助 GC
        }

        x.item = null;
        size--;
        return element;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
        return node(index).item;
    }

    /**
     * 根据索引查找节点（性能优化版）
     * <p>
     * 利用位运算 (size >> 1) 判断索引在前半段还是后半段，
     * 从而决定是从头遍历还是从尾遍历，将查找时间减半。
     *
     * @param index 索引
     * @return 对应的节点对象
     */
    Node<E> node(int index) {
        // 如果索引在前半段
        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            // 如果索引在后半段
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    @Override
    public void printLinkList() {
        if (this.size == 0) {
            System.out.println("链表为空");
        } else {
            // 生产环境建议移除或改用 StringBuilder，避免大量 String 拼接
            Node<E> temp = first;
            System.out.print("目前的列表，头节点：" + first.item + " 尾节点：" + last.item + " 整体：");
            while (temp != null) {
                System.out.print(temp.item + "，");
                temp = temp.next;
            }
            System.out.println();
        }
    }

    /**
     * 内部节点类
     * 定义为 protected 允许包内其他类（如迭代器或工具类）访问
     */
    protected static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        public Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
}
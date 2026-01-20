package cn.xuele.types.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MQ 消息事件基础抽象类
 * <p>
 * 作用：定义所有 MQ 消息的统一规范，确保发送的消息都有标准的外壳（Envelope）。
 *
 * @param <T> 消息体内具体的业务数据类型 (Payload)
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 11:07
 */
public abstract class BaseEvent<T> {

    /**
     * 构建标准的消息体
     * 将业务数据封装进统一的“信封”中，自动补充ID和时间戳
     *
     * @param data 具体的业务数据
     * @return 包含元数据的完整消息对象
     */
    public abstract EventMessage<T> buildEventMessage(T data);

    /**
     * 获取该事件对应的路由键 (Routing Key)
     * 生产者发送消息时使用
     *
     * @return RabbitMQ RoutingKey 字符串
     */
    public abstract String topic();

    /**
     * 统一的消息体结构（传输对象）
     * 这是真正序列化为 JSON 发送到 MQ 的对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMessage<T> {
        /**
         * 消息唯一标识 ID
         * 用于消息追踪、幂等性处理（防止重复消费）
         */
        private String id;

        /**
         * 消息生成时间戳
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime timeStamp;

        /**
         * 实际的业务数据载体
         */
        private T data;
    }
}
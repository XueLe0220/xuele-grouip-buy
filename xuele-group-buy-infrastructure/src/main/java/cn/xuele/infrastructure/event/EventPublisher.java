package cn.xuele.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息发布者 (Event Publisher)
 * <p>
 * 作用：封装底层的 RabbitTemplate 调用，提供统一的消息发送接口。
 * 业务层只需调用 publish 方法，无需关心具体的 Exchange 配置和序列化细节。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 12:03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 从配置文件读取交换机名称
     * 这样做的好处是：如果未来要换交换机，只需改 YAML，不用改代码
     */
    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    /**
     * 发送 MQ 消息（标准通用方法）
     *
     * @param routingKey 路由键 (决定消息发往哪个队列)
     * @param message    消息体 (通常是 JSON 字符串)
     */
    public void publish(String routingKey, String message) {
        try {
            // 核心发送逻辑：convertAndSend(交换机, 路由键, 消息内容, 后置处理器)
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message, m -> {
                // 关键设置：设置消息投递模式为 PERSISTENT (持久化)
                // 作用：告诉 MQ 把消息写入磁盘。即使 MQ 服务重启，只要队列也是持久化的，消息就不会丢。
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return m;
            });

            // 正常日志可以设为 debug 级别，避免生产环境日志过多
            log.info("MQ消息发送成功 [Key:{}]", routingKey);

        } catch (Exception e) {
            // 异常处理：记录详细错误日志（包含 Key 和 消息内容，方便排查）
            log.error("MQ发送失败 [Key:{}] msg:{}", routingKey, message, e);

            // 重要：抛出异常
            // 作用：让上层业务感知到发送失败，从而触发数据库事务回滚，保证数据一致性。
            throw e;
        }
    }
}
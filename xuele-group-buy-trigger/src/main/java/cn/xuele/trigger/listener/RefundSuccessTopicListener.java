package cn.xuele.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 拼团退单事件消费者
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/24 14:26
 */
@Slf4j
@Component
public class RefundSuccessTopicListener {

    @RabbitListener(queues = "${spring.rabbitmq.config.producer.topic_refund.queue}")
    public void listener(String message) {
        try {
            log.info("📧 收到退单MQ消息: {}", message);
            // 这里解析消息，去调微信/支付宝接口
        } catch (Exception e) {
            log.error("消费失败", e);
        }
    }
}

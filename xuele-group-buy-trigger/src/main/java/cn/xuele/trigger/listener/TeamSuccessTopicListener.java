package cn.xuele.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 拼团成功消息消费者
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20
 */
@Slf4j
@Component
public class TeamSuccessTopicListener {

    /**
     * 简化版监听
     * 直接引用配置文件中的队列名
     * 前提：该队列必须已经在 RabbitMQConfig 中声明过
     */
    @RabbitListener(queues = "${spring.rabbitmq.config.producer.topic_team_success.queue}")
    public void listener(String message) {
        log.info("MQ接收消息 [拼团成功] content: {}", message);

    }
}
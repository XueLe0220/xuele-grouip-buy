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

    @RabbitListener(queues = "${spring.rabbitmq.config.producer.topic_team_success.queue}")
    public void listener(String message) {
        log.info("【MQ】接收消息 【拼团成功】 content: {}", message);

    }
}
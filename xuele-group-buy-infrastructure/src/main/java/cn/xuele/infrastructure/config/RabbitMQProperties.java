package cn.xuele.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQ消息配置属性
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/24 13:49
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.rabbitmq.config.producer")
public class RabbitMQProperties {

    private String exchange;

    private TopicConfig topicTeamSuccess;

    private TopicConfig topicRefund;

    /**
     * 内部类，对应 routing_key 和 queue 的结构
     */
    @Data
    public static class TopicConfig {
        private String routingKey;
        private String queue;
    }
}

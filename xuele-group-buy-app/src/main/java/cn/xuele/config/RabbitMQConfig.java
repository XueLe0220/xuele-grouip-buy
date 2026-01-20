package cn.xuele.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置类
 * 负责声明交换机、队列以及它们之间的绑定关系
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20
 */
@Configuration
public class RabbitMQConfig {

    /* 读取配置文件中的自定义参数 */

    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String teamSuccessRoutingKey;

    @Value("${spring.rabbitmq.config.producer.topic_team_success.queue}")
    private String teamSuccessQueueName;

    /**
     * 1. 定义拼团交易专用交换机 (Topic类型)
     * durable=true: 持久化，重启不丢失
     * autoDelete=false: 长期使用，不自动删除
     */
    @Bean
    public TopicExchange groupBuyExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * 2. 定义拼团成功队列
     * 独立定义Bean，确保Spring能自动在MQ服务端创建该队列
     */
    @Bean
    public Queue groupBuyTeamSuccessQueue() {
        return new Queue(teamSuccessQueueName, true);
    }

    /**
     * 3. 绑定队列到交换机
     * 规则：当消息的 RoutingKey 匹配时，投递到该队列
     */
    @Bean
    public Binding teamSuccessBinding() {
        return BindingBuilder.bind(groupBuyTeamSuccessQueue())
                .to(groupBuyExchange())
                .with(teamSuccessRoutingKey);
    }
}
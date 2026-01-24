package cn.xuele.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    // 1.
    private final RabbitMQProperties properties;


    @Bean
    public TopicExchange groupBuyExchange() {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue groupBuyTeamSuccessQueue() {
        return new Queue(properties.getTopicTeamSuccess().getQueue(), true);
    }

    @Bean
    public Queue groupBuyRefundQueue() {
        return new Queue(properties.getTopicRefund().getQueue(), true);
    }

    @Bean
    public Binding teamSuccessBinding() {
        return BindingBuilder.bind(groupBuyTeamSuccessQueue())
                .to(groupBuyExchange())
                .with(properties.getTopicTeamSuccess().getRoutingKey());
    }

    @Bean
    public Binding refundBinding() {
        return BindingBuilder.bind(groupBuyRefundQueue())
                .to(groupBuyExchange())
                .with(properties.getTopicRefund().getRoutingKey());
    }
}
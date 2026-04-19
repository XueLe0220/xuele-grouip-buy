package cn.xuele.trigger.listener;

import cn.xuele.domain.trade.model.entity.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.service.ITradeRefundOrderService;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TeamSuccessTopicListener {

    private final ITradeRefundOrderService tradeRefundOrderService;

    @RabbitListener(queues = "${spring.rabbitmq.config.producer.topic_team_success.queue}")
    public void listener(String message) {
        log.info("接收消息（退单成功）- 恢复拼团队伍锁单量:{}", message);
        TeamRefundSuccessEvent teamRefundSuccessEvent = JSON.parseObject(message, TeamRefundSuccessEvent.class);
        try {
            tradeRefundOrderService.restoreTeamLockStock(teamRefundSuccessEvent);
        } catch (Exception e) {
            log.info("接收消息（退单成功）- 恢复拼团队伍锁单量失败:{}", message, e);
            // 抛异常，mq消息会重试
            throw new RuntimeException(e);
        }
    }
}
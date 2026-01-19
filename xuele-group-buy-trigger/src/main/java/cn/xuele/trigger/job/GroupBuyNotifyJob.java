package cn.xuele.trigger.job;

import cn.xuele.domain.trade.service.settlement.ITradeSettlementOrderService;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 拼团完结回调通知任务
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 15:36
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyNotifyJob {

    private final ITradeSettlementOrderService tradeSettlementOrderService;

    // 每 15 秒执行一次
    @Scheduled(cron = "0/15 * * * * ?")
    public void exec() {
        try {
            Map<String, Integer> result = tradeSettlementOrderService.executeSettlementNotifyTask();

            if (result != null && result.getOrDefault("total", 0) > 0) {
                log.info("【定时任务】拼团回调通知完成 result:{}", JSON.toJSONString(result));
            }

        } catch (Exception e) {
            log.error("【定时任务】拼团回调通知异常", e);
        }
    }
}
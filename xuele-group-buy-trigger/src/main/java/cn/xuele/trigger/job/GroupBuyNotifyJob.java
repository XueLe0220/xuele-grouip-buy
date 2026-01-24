package cn.xuele.trigger.job;

import cn.xuele.domain.trade.service.ITradeSettlementOrderService;
import cn.xuele.domain.trade.service.ITradeTaskService;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private final ITradeTaskService tradeTaskService;
    private final RedissonClient redissonClient;
    private static final String NOTIFY_JOB_EXEC = "group_buy_market_notify_job_exec";

    // 每 15 秒执行一次
    @Scheduled(cron = "0/15 * * * * ?")
    public void exec() {
        RLock lock = redissonClient.getLock(NOTIFY_JOB_EXEC);
        try {
            boolean isLocked = lock.tryLock(0, -1 , TimeUnit.SECONDS);
            if(!isLocked) return;
            Map<String, Integer> result = tradeTaskService.execNotifyJob();

            if (result != null && result.getOrDefault("total", 0) > 0) {
                log.info("【定时任务】拼团回调通知完成 result:{}", JSON.toJSONString(result));
            }

        } catch (Exception e) {
            log.error("【定时任务】拼团回调通知异常", e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
}
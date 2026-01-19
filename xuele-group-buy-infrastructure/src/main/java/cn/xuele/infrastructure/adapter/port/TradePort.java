package cn.xuele.infrastructure.adapter.port;

import cn.xuele.domain.trade.adapter.port.ITradePort;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.infrastructure.gateway.GroupBuyNotifyService;
import cn.xuele.types.enums.NotifyTaskHTTPEnumVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:16
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TradePort implements ITradePort {

    private final RedissonClient redissonClient;
    private final GroupBuyNotifyService groupBuyNotifyService;
    private static final String NOTIFY_TASK_JOB_KEY = "notify_job_lock_key_";

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTask) {
        RLock lock = redissonClient.getLock(NOTIFY_TASK_JOB_KEY + notifyTask.getTeamId());

        try {
            if (lock.tryLock(0, -1, TimeUnit.SECONDS)) {

                try {
                    if (StringUtils.isBlank(notifyTask.getNotifyUrl()) || "暂无".equals(notifyTask.getNotifyUrl())) {
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }

                    return groupBuyNotifyService.groupBuyNotify(notifyTask.getNotifyUrl(),
                            notifyTask.getParameterJson());
                } finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }

            }
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        }

    }
}

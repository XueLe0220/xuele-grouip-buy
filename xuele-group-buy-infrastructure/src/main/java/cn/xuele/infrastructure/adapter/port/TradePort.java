package cn.xuele.infrastructure.adapter.port;

import cn.xuele.domain.trade.adapter.port.ITradePort;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.xuele.infrastructure.event.EventPublisher;
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
 * 交易结算防腐层端口实现 (Infrastructure Adapter)
 * <p>
 * 职责：
 * 1. 承接领域层的通知请求，适配到底层网关。
 * 2. 【核心】实现分布式锁机制，防止多节点并发重复执行任务。
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
    private final EventPublisher publisher;

    /**
     * 分布式锁 Key 前缀
     */
    private static final String NOTIFY_TASK_JOB_KEY = "notify_job_lock_key_";

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTask) {
        // 1. 获取分布式锁实例 (锁粒度：TeamId)
        RLock lock = redissonClient.getLock(NOTIFY_TASK_JOB_KEY + notifyTask.getUuid());

        try {
            // 2. 尝试抢占锁 (非阻塞模式 Fail-fast)
            // waitTime = 0: 抢不到立刻返回 false，不阻塞等待 (避免堆积线程)
            // leaseTime = -1: 开启 WatchDog 看门狗机制，自动续期，防止业务执行时间过长导致锁释放
            if (lock.tryLock(0, -1, TimeUnit.SECONDS)) {

                try {
                    // HTTP调用
                    if (NotifyTypeEnumVO.HTTP.getCode().equals(notifyTask.getNotifyType())) {

                        // 校验参数 (防御性编程)
                        if (StringUtils.isBlank(notifyTask.getNotifyUrl()) || "暂无".equals(notifyTask.getNotifyUrl())) {
                            log.warn("回调地址为空，跳过执行 teamId:{}", notifyTask.getTeamId());
                            return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                        }
                        // 执行真正的 HTTP 网关调用
                        return groupBuyNotifyService.groupBuyNotify(notifyTask.getNotifyUrl(),
                                notifyTask.getParameterJson());
                    }

                    // MQ调用
                    if (NotifyTypeEnumVO.MQ.getCode().equals(notifyTask.getNotifyType())) {
                        publisher.publish(notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }

                } finally {
                    // 5. 安全释放锁 (只能释放自己持有的锁)
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }

            }
            // 6. 抢锁失败，说明其他节点正在处理，直接返回 NULL 状态，本次跳过
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            // 恢复中断状态，防止吞掉中断信号
            Thread.currentThread().interrupt();
            log.error("分布式锁处理异常 teamId:{}", notifyTask.getTeamId(), e);
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        }
    }
}
package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.TeamRefundSuccessEvent;
import cn.xuele.domain.trade.service.ITradeTaskService;
import cn.xuele.domain.trade.service.lock.fatcory.TradeLockRuleFilterFactory;
import cn.xuele.domain.trade.service.refund.business.IRefundOrderStrategy;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 退单策略抽象基类
 * 提供共用的依赖注入和MQ消息发送功能
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/04/19 16:15
 */
@Slf4j
public abstract class AbstractRefundOrderStrategy implements IRefundOrderStrategy {

    @Resource
    protected ITradeRepository repository;

    @Resource
    protected ITradeTaskService tradeTaskService;

    @Resource
    protected ThreadPoolExecutor threadPoolExecutor;

    /**
     * 异步发送MQ消息
     * @param notifyTaskEntity 通知任务实体
     * @param refundType 退单类型描述
     */
    protected void sendRefundNotifyMessage(NotifyTaskEntity notifyTaskEntity, String refundType) {
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知交易退单({}) result:{}", refundType, JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知交易退单失败({}) result:{}", refundType, JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

    /**
     * 通用库存恢复逻辑
     * @param teamRefundSuccessEvent 团队退单成功信息
     * @param refundType 退单类型描述
     * @throws Exception 异常
     */
    protected void doReverseStock(TeamRefundSuccessEvent teamRefundSuccessEvent, String refundType) throws Exception {
        log.info("退单；恢复锁单量 - {} {} {} {}", refundType, teamRefundSuccessEvent.getUserId(), teamRefundSuccessEvent.getActivityId(), teamRefundSuccessEvent.getTeamId());
        // 1. 恢复库存key
        String recoveryTeamStockKey = TradeLockRuleFilterFactory.generateRecoveryTeamStockKey(teamRefundSuccessEvent.getActivityId(), teamRefundSuccessEvent.getTeamId());
        // 2. 退单恢复库存
        repository.refund2AddRecovery(recoveryTeamStockKey, teamRefundSuccessEvent.getOrderId());
    }

}
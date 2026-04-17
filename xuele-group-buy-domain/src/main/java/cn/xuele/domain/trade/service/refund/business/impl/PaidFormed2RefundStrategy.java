package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.TeamRefundEvent;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.service.ITradeTaskService;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 发起退单（已成团&已支付），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:54
 */
@Slf4j
@Service("paidFormed2RefundStrategy")
@RequiredArgsConstructor
public class PaidFormed2RefundStrategy implements IRefundStrategy {

    private final ITradeRepository repository;
    private final ITradeTaskService tradeTaskService;
    private final ThreadPoolExecutor threadPoolExecutor;


    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {

        // 1. 退单
        NotifyTaskEntity notifyTask =
                repository.paidFormed2Refund(GroupBuyRefundAggregate.buildPaidFormed2RefundAggregate(tradeRefundOrderEntity,
                -1, -1, GroupBuyTeamStatusVO.COMPLETE_FAIL));

        // 2. 异步发送回调通知
        if (null != notifyTask) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTask);
                    log.info("回调通知交易退单(已支付，已成团) result:{}", JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知交易退单失败(已支付，已成团) result:{}", JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

    @Override
    public void reverseStock(TeamRefundEvent teamRefundEvent) throws Exception {
        log.info("退单；已支付、已成团，队伍组队结束，不需要恢复锁单量 {} {} {}", teamRefundEvent.getUserId(), teamRefundEvent.getActivityId(), teamRefundEvent.getTeamId());
    }
}

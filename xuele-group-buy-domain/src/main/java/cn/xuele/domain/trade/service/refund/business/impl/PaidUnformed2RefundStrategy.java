package cn.xuele.domain.trade.service.refund.business.impl;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.service.ITradeTaskService;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 发起退单（未成团&已支付），锁单量-1、完成量-1、组队订单状态更新、发送退单消息（MQ）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:54
 */
@Slf4j
@Service("paidUnformed2RefundStrategy")
@RequiredArgsConstructor
public class PaidUnformed2RefundStrategy implements IRefundStrategy {

    private final ITradeRepository repository;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ITradeTaskService tradeTaskService;

    @Override
    public void refund(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("逆向流程-退单策略(已支付未成团) userId:{} orderId:{}",
                tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());

        // 1. [核心] 交易退单 + 保存任务单 (原子操作)
        // 这一步必须保证：订单状态变更 和 NotifyTask 落库 在同一个事务中完成
        NotifyTaskEntity notifyTask = repository.paidUnformed2Refund(
                GroupBuyRefundAggregate.buildPaidUnformed2RefundAggregate(tradeRefundOrderEntity, -1, -1)
        );

        if (null == notifyTask) {
            log.error("逆向流程-数据库更新失败(可能订单不存在或状态不匹配) orderId:{}", tradeRefundOrderEntity.getOrderId());
            // 抛出异常，中断流程，让上层感知到失败
            throw new AppException(ResponseCode.E0009.getCode(), ResponseCode.E0009.getInfo());
        }

        // 2. [辅助] 异步发送 MQ 消息 (尽最大努力通知)
        threadPoolExecutor.execute(() -> {
            try {
                // 执行通知任务（发送 MQ）
                Map<String, Integer> notifyResultMap = tradeTaskService.execNotifyJob(notifyTask);
                log.info("回调通知交易退单（已支付、未成团）-异步通知发送成功 userId:{} result:{}", tradeRefundOrderEntity.getUserId(),
                        JSON.toJSONString(notifyResultMap));
            } catch (Exception e) {
                log.error("回调通知交易退单（已支付、未成团）-异步通知发送失败(等待定时任务补偿) userId:{} orderId:{}",
                        tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId(), e);
            }
        });
    }
}

package cn.xuele.domain.trade.service;

import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

/**
 * 交易任务服务类（MQ/HTTP）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/24 12:18
 */
public interface ITradeTaskService {
    /**
     * 执行结算通知任务
     */
    Map<String, Integer> execNotifyJob() throws Exception;

    /**
     * 执行结算通知任务
     */
    Map<String, Integer> execNotifyJob(String teamId) throws Exception;

    /**
     * 执行结算通知任务
     */
    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTask) throws Exception;
}

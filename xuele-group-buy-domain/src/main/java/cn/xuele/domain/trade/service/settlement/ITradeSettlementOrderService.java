package cn.xuele.domain.trade.service.settlement;

import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementEntity;

import java.util.Map;

/**
 * 交易结算领域服务接口
 * <p>
 * 职责：负责接收支付渠道的通知，完成拼团订单的最终结算（扣减库存/修改状态/成团判定）。
 *
 * @author XueLe
 * @since 2026/01/18
 */
public interface ITradeSettlementOrderService {

    /**
     * 结算营销拼团订单
     * <p>
     * 1. 幂等性保障：支持重复调用，重复调用时应直接返回成功结果或当前最新状态。
     * 2. 资金校验：必须校验回调金额与订单金额是否一致。
     * 3. 事务性：方法执行成功即代表数据库落库完成。
     *
     * @param tradePaySettlementEntity 支付回调上下文 (包含外部单号、金额、支付时间)
     * @return TradeSettlementEntity 结算回执 (包含内部单号、是否成团)
     */
    TradeSettlementEntity settlement(TradePaySettlementEntity tradePaySettlementEntity) throws Exception;


    /**
     * 执行全量结算通知任务（定时任务入口）
     * <p>
     * 场景说明：
     * 通常由分布式调度中心 (如 XXL-JOB, Elastic-Job) 定时触发 (例如每1分钟执行一次)。
     * 该方法会扫描 {@code notify_task} 表中状态为 {@code INIT(0)} 或 {@code RETRY(2)} 的记录。
     * <p>
     * <strong>注意：</strong>
     * 实现层需考虑“深分页”或“流式查询”问题，避免一次加载过多任务导致 OOM。
     *
     * @return 执行结果统计 Map，约定 Key 如下：
     * <ul>
     * <li>{@code "total"} : Integer - 本次扫描到的任务总数</li>
     * <li>{@code "success"} : Integer - 通知成功并收到 200 OK 的数量</li>
     * <li>{@code "fail"} : Integer - 通知失败或进入重试队列的数量</li>
     * </ul>
     */
    Map<String, Integer> executeSettlementNotifyTask() throws Exception;

    /**
     * 执行指定拼团 ID 的结算通知任务（人工补偿/测试入口）
     * <p>
     * 场景说明：
     * 1. 生产环境告警后，运维人员通过管理后台手动触发特定拼团的补单通知。
     * 2. 消息队列 (MQ) 消费失败进入死信队列后，通过此接口进行单据维度的重试。
     * <p>
     * <strong>幂等性：</strong>
     * 该接口需保证幂等性。如果任务当前状态已经是 {@code SUCCESS}，应直接返回成功或忽略，
     * 不可重复发送通知以免造成下游重复业务处理。
     *
     * @param teamId 拼团组队 ID (对应 {@code notify_task.team_id}，非空)
     * @return 执行结果统计 Map (通常 total 为 1)
     */
    Map<String, Integer> executeSettlementNotifyTask(String teamId) throws Exception;
}
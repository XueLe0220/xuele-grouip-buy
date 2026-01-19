package cn.xuele.domain.trade.service.settlement;

import cn.xuele.domain.trade.adapter.port.ITradePort;
import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.xuele.domain.trade.model.entity.*;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import cn.xuele.types.enums.NotifyTaskHTTPEnumVO;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拼团交易结算领域服务实现
 * <p>
 * 职责：编排结算流程，连接规则引擎与基础设施层。
 * 核心流程：
 * 1. 规则校验 (Rule Engine)：检查黑名单、幂等性、时效性，并加载拼团快照。
 * 2. 聚合构建 (Aggregate Build)：将分散的“人、钱、团”组装成一致性聚合根。
 * 3. 仓储落库 (Repository Save)：执行原子性更新。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeSettlementOrderService implements ITradeSettlementOrderService {

    private final ITradeRepository repository;
    private final ITradePort port;

    // 注入我们在 Factory 里定义的规则链
    private final BusinessLinkedList<TradeSettlementRuleCommandEntity,
            TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeSettlementEntity settlement(TradePaySettlementEntity tradePaySettlementEntity) throws Exception {

        log.info("拼团交易-结算开始 userId:{} outTradeNo:{}", tradePaySettlementEntity.getUserId(),
                tradePaySettlementEntity.getOutTradeNo());

        // 1. 执行结算规则链 (校验 + 数据查询)
        // 这一步会经过：黑名单 -> 外部单号 -> 时间校验 -> EndFilter(组装数据)
        TradeSettlementRuleFilterBackEntity backEntity = tradeSettlementRuleFilter.apply(
                TradeSettlementRuleCommandEntity.builder()
                        .source(tradePaySettlementEntity.getSource())
                        .channel(tradePaySettlementEntity.getChannel())
                        .outTradeNo(tradePaySettlementEntity.getOutTradeNo())
                        .outTradeTime(tradePaySettlementEntity.getOutTradeTime())
                        .userId(tradePaySettlementEntity.getUserId())
                        .build(),
                new TradeSettlementRuleFilterFactory.DynamicContext()); // 创建空的上下文供链条传递

        // 2. 还原拼团组队实体 (利用规则引擎查出来的最新数据)
        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder()
                .teamId(backEntity.getTeamId())
                .activityId(backEntity.getActivityId())
                .targetCount(backEntity.getTargetCount())
                .completeCount(backEntity.getCompleteCount())
                .lockCount(backEntity.getLockCount())
                .status(backEntity.getStatus())
                .validStartTime(backEntity.getValidStartTime())
                .validEndTime(backEntity.getValidEndTime())
                .notifyUrl(backEntity.getNotifyUrl())
                .build();

        // 3. 构建聚合根 (这是数据一致性的最小单元)
        GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(UserEntity.builder().userId(tradePaySettlementEntity.getUserId()).build())
                .tradePaySettlementEntity(tradePaySettlementEntity)
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .build();

        // 4. 拼团交易结算 (原子操作)
        boolean isNotify = repository.settlement(groupBuyTeamSettlementAggregate);

        // 5. 组队回调处理
        if (isNotify) {
            Map<String, Integer> notifyResultMap = executeSettlementNotifyTask(backEntity.getTeamId());
            log.info("回调通知拼团完结 result:{}", JSON.toJSONString(notifyResultMap));
        }


        log.info("拼团交易-结算完成 userId:{} teamId:{}", tradePaySettlementEntity.getUserId(), backEntity.getTeamId());
        // 6. 返回结算回执
        return TradeSettlementEntity.builder()
                .source(tradePaySettlementEntity.getSource())
                .channel(tradePaySettlementEntity.getChannel())
                .userId(tradePaySettlementEntity.getUserId())
                .teamId(backEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(tradePaySettlementEntity.getOutTradeNo())
                .build();
    }

    @Override
    public Map<String, Integer> executeSettlementNotifyTask() {
        log.info("拼团交易-执行结算通知任务");

        // 查询未执行任务
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList();

        return executeSettlementNotifyTask(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> executeSettlementNotifyTask(String teamId) {
        // 1. 日志：明确入参
        log.info("执行指定拼团结算通知任务, teamId: {}", teamId);

        // 2. 查库：这里后续要在 Repository 实现中注意，必须查不到返回空List，不能返回null，防止下面空指针
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList(teamId);

        // 3. 执行
        return executeSettlementNotifyTask(notifyTaskEntityList);
    }

    private Map<String, Integer> executeSettlementNotifyTask(List<NotifyTaskEntity> notifyTaskEntityList) {
        Map<String, Integer> resultMap = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int retryCount = 0;

        if (notifyTaskEntityList == null || notifyTaskEntityList.isEmpty()) {
            return resultMap;
        }

        for (NotifyTaskEntity notifyTask : notifyTaskEntityList) {
            try {
                // 执行 HTTP 通知
                String response = port.groupBuyNotify(notifyTask);

                // 1. 成功
                if (NotifyTaskHTTPEnumVO.SUCCESS.getCode().equals(response)) {
                    int updateCount = repository.updateNotifyTaskStatusSuccess(notifyTask.getTeamId());
                    if (updateCount > 0) successCount++;
                }
                // 2. 失败 (业务逻辑层面的失败，如 404/500)
                else {
                    // 内存先自增，保证逻辑闭环
                    notifyTask.increaseRetryCount();

                    if (notifyTask.hasRetryChance()) {
                        // 此时 task 里的 count 已经是+1后的值了
                        int updateCount = repository.updateNotifyTaskStatusRetry(notifyTask.getTeamId());
                        if (updateCount > 0) retryCount++;
                    } else {
                        int updateCount = repository.updateNotifyTaskStatusError(notifyTask.getTeamId());
                        if (updateCount > 0) failCount++;
                    }
                }
            } catch (Exception e) {
                // 3. 异常 (代码执行层面的失败，如超时、NPE)
                log.error("结算通知任务执行异常, teamId: {}", notifyTask.getTeamId(), e);

                // 异常情况下，也要更新数据库，否则会死循环或状态丢失
                notifyTask.increaseRetryCount();
                try {
                    // 同样判断是否还能重试
                    if (notifyTask.hasRetryChance()) {
                        repository.updateNotifyTaskStatusRetry(notifyTask.getTeamId());
                        retryCount++;
                    } else {
                        repository.updateNotifyTaskStatusError(notifyTask.getTeamId());
                        failCount++;
                    }
                } catch (Exception ex) {
                    log.error("数据库更新异常, teamId: {}", notifyTask.getTeamId(), ex);
                }
            }
        }

        resultMap.put("total", notifyTaskEntityList.size());
        resultMap.put("success", successCount);
        resultMap.put("fail", failCount);
        resultMap.put("retry", retryCount);

        return resultMap;
    }
}
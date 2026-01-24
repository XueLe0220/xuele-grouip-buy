package cn.xuele.domain.trade.service.settlement;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.xuele.domain.trade.model.entity.*;
import cn.xuele.domain.trade.service.ITradeSettlementOrderService;
import cn.xuele.domain.trade.service.ITradeTaskService;
import cn.xuele.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ITradeTaskService tradeTaskService;

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
                .notifyConfigVO(backEntity.getNotifyConfigVO())
                .build();

        // 3. 构建聚合根 (这是数据一致性的最小单元)
        GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(UserEntity.builder().userId(tradePaySettlementEntity.getUserId()).build())
                .tradePaySettlementEntity(tradePaySettlementEntity)
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .build();

        // 4. 拼团交易结算 (原子操作)
        NotifyTaskEntity notifyTaskEntity = repository.settlement(groupBuyTeamSettlementAggregate);

        // 5. 组队回调处理
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知拼团完结 result:{}", JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知拼团完结失败 result:{}", JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
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


}
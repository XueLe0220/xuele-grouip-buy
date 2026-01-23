package cn.xuele.domain.trade.service.lock.filter;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.xuele.domain.trade.service.lock.fatcory.TradeLockRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 交易规则过滤 - 组队库存抢占校验
 * <p>
 * 采用 Redis 原子递增（INCR）机制进行库存预扣减，
 * 解决高并发场景下的数据库行锁瓶颈，实现无锁化设计。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 12:02
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeamStockOccupyRuleFilter implements ILogicHandler
        <TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    private final ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter,
                                               TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-组队库存校验 userId:{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        String teamId = requestParameter.getTeamId();
        // 1. 团长开团（teamId为空）无需抢占拼团库存，直接放行
        if (StringUtils.isBlank(teamId)) {
            return next(requestParameter, dynamicContext);
        }

        // 2. 获取活动配置及生成Key
        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivityEntity();
        String teamStockKey = dynamicContext.generateTeamStockKey(teamId);
        String recoveryTeamStockKey = dynamicContext.generateRecoveryTeamStockKey(teamId);

        // 3. 设置上下文，便于后续链路（如数据库回滚）获取恢复Key
        dynamicContext.setRecoveryTeamStockKey(recoveryTeamStockKey);

        // 4. 执行 Redis 库存抢占（Fail-Fast 策略）
        boolean status = repository.occupyTeamStock(
                teamStockKey,
                recoveryTeamStockKey,
                groupBuyActivity.getTargetCount(),
                groupBuyActivity.getValidTime());

        // 5. 抢占失败，抛出异常阻断后续数据库操作
        if (!status) {
            log.warn("交易规则过滤-组队库存校验失败（库存不足） userId:{} activityId:{} teamStockKey:{}",
                    requestParameter.getUserId(), requestParameter.getActivityId(), teamStockKey);
            throw new AppException(ResponseCode.E0008);
        }

        // 6. 抢占成功，进入下一节点
        return next(requestParameter, dynamicContext);
    }
}
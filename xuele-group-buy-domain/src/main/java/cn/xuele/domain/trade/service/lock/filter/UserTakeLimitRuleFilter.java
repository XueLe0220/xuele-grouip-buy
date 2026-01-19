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
import org.springframework.stereotype.Service;

/**
 * 交易规则过滤 - 用户限购校验
 * <p>
 * 职责：校验当前用户是否已达到该活动的个人参与上限（如：每人限购 3 单）。
 * 依赖：依赖上一节点（ActivityUsabilityRuleFilter）在 Context 中注入的活动配置。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:49
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    private final ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-用户参与次数校验 userId:{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 1. 从上下文中获取活动配置（上一节点塞进去的）
        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivityEntity();

        // 2. 校验配置：如果活动未配置限购次数，则直接放行
        if (null == groupBuyActivity.getTakeLimitCount()) {
            return next(requestParameter, dynamicContext);
        }

        // 3. 【核心逻辑修复】查询当前用户已购买数量
        // 注意：这里必须带上 userId，否则查的是全网总销量，逻辑就变成了“活动总限购”而不是“个人限购”
        Integer userTakeCount = repository.queryOrderCountByActivityIdAndUserId(requestParameter.getActivityId(), requestParameter.getUserId());

        // 4. 校验是否超限
        if (userTakeCount >= groupBuyActivity.getTakeLimitCount()) {
            log.info("用户参与次数校验未通过，已达个人上限 userId:{} count:{} limit:{}",
                    requestParameter.getUserId(), userTakeCount, groupBuyActivity.getTakeLimitCount());
            throw new AppException(ResponseCode.E0103);
        }

        dynamicContext.setUserTakeCount(userTakeCount);

        return next(requestParameter, dynamicContext);
    }
}
package cn.xuele.domain.trade.service.lock.filter;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.xuele.domain.trade.service.lock.fatcory.TradeLockRuleFilterFactory;
import cn.xuele.types.design.framework.link.handler.ILogicHandler;
import cn.xuele.types.enums.ActivityStatusEnumVO;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 交易规则过滤 - 活动可用性校验
 * <p>
 * 职责：作为交易链路的“首关”，负责从数据库加载活动详情，并校验活动的基础状态（状态枚举、有效期）。
 * 优化：加载后的活动对象会写入 DynamicContext，供下游规则（如限购、库存）直接使用，避免重复查库。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 14:35
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    // 【修复】必须加 final，否则 @RequiredArgsConstructor 不会生成构造注入，导致空指针
    private final ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-活动可用性校验 userId:{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 1. 查询活动详情
        GroupBuyActivityEntity groupBuyActivity = repository.queryGroupBuyActivityByActivityId(requestParameter.getActivityId());

        // 校验：活动是否存在
        if (groupBuyActivity == null) {
            throw new AppException(ResponseCode.E0101.getCode(), "活动不存在");
        }

        // 2. 校验活动状态 (非生效状态直接拦截)
        if (!ActivityStatusEnumVO.EFFECTIVE.equals(groupBuyActivity.getStatus())) {
            log.warn("活动可用性校验未通过，非生效状态 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0101);
        }

        // 3. 校验活动时间 (不在开始和结束范围内)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(groupBuyActivity.getStartTime()) || now.isAfter(groupBuyActivity.getEndTime())) {
            log.warn("活动可用性校验未通过，非可参与时间范围 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0102);
        }

        // 4. 【关键】将活动信息写入上下文，透传给后续节点
        dynamicContext.setGroupBuyActivityEntity(groupBuyActivity);

        // 5. 执行下一个责任链节点
        return next(requestParameter, dynamicContext);
    }
}
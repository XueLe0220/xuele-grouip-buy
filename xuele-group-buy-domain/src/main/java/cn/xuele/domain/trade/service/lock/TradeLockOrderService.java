package cn.xuele.domain.trade.service.lock;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyLockOrderAggregate;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.domain.trade.service.lock.fatcory.TradeLockRuleFilterFactory;
import cn.xuele.types.design.framework.link.chain.BusinessLinkedList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 交易订单领域服务实现类
 * <p>
 * 职责：
 * 作为领域层的核心业务入口，负责调度仓储层（Repository）完成业务逻辑。
 * 在 DDD 中，它负责组装聚合根（Aggregate Root），并调用仓储进行持久化。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 16:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeLockOrderService implements ITradeLockOrderService {

    // 注入仓储接口（依赖倒置：只依赖接口，不依赖 Infra 层的具体实现）
    private final ITradeRepository tradeRepository;

    private final BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext,
            TradeLockRuleFilterBackEntity> tradeLockRuleFilter;

    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        log.info("拼团锁单-查询未支付订单 userId:{} outTradeNo:{}", userId, outTradeNo);
        return tradeRepository.queryNoPayMarketPayOrderByOutTradeNo(userId, outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        log.info("拼团锁单-查询拼团进度 teamId:{}", teamId);
        return tradeRepository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity,
                                                   PayDiscountEntity payDiscountEntity) throws Exception {
        log.info("拼团锁单-开始锁定 user:{} activity:{} discount:{}",
                userEntity.getUserId(), payActivityEntity.getActivityId(), payDiscountEntity.getDeductionPrice());

        // 交易规则过滤
        TradeLockRuleFilterBackEntity tradeRuleFilterBackEntity = tradeLockRuleFilter.apply(TradeLockRuleCommandEntity.builder()
                .activityId(payActivityEntity.getActivityId())
                .userId(userEntity.getUserId())
                .build(),
                new TradeLockRuleFilterFactory.DynamicContext());

        Integer userTakeOrderCount = tradeRuleFilterBackEntity.getUserTakeOrderCount();

        // 1. 组装聚合根 (Aggregate)
        // 这是 DDD 的核心步骤：将分散的实体打包成一个具有完整业务语义的聚合对象。
        GroupBuyLockOrderAggregate groupBuyOrderAggregate = GroupBuyLockOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userTakeOrderCount(userTakeOrderCount)
                .build();

        // 2. 调用仓储层进行事务处理
        // 具体的“防超卖”、“新团/旧团判断”、“数据库原子更新”逻辑，全部封装在 Repository 中。
        try{
            return tradeRepository.lockMarketPayOrder(groupBuyOrderAggregate);
        }catch (Exception e){
            tradeRepository.recoveryTeamStock(tradeRuleFilterBackEntity.getRecoveryTeamStockKey(), payActivityEntity.getValidTime());
            throw e;
        }
    }
}
package cn.xuele.infrastructure.adapter.repository;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyLockOrderAggregate;
import cn.xuele.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import cn.xuele.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.xuele.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.domain.trade.model.valobj.NotifyConfigVO;
import cn.xuele.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.xuele.infrastructure.dao.IGroupBuyActivityDao;
import cn.xuele.infrastructure.dao.IGroupBuyOrderDao;
import cn.xuele.infrastructure.dao.IGroupBuyOrderListDao;
import cn.xuele.infrastructure.dao.INotifyTaskDao;
import cn.xuele.infrastructure.dao.po.GroupBuyOrder;
import cn.xuele.infrastructure.dao.po.GroupBuyOrderList;
import cn.xuele.infrastructure.dao.po.GroupBuyActivity;
import cn.xuele.infrastructure.dao.po.NotifyTask;
import cn.xuele.infrastructure.dcc.DCCService;
import cn.xuele.types.common.Constants;
import cn.xuele.types.enums.ActivityStatusEnumVO;
import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 交易仓储实现类 (Infrastructure Layer)
 * <p>
 * 核心职责：
 * 1. 实现领域层定义的 ITradeRepository 接口。
 * 2. 充当“适配器 (Adapter)”，将领域对象 (Entity/Aggregate) 转换为数据库对象 (PO)，反之亦然。
 * 3. 处理具体的数据库操作细节 (MyBatis DAO)。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:03
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TradeRepository implements ITradeRepository {

    private final IGroupBuyActivityDao groupBuyActivityDao;
    private final IGroupBuyOrderDao groupBuyOrderDao;
    private final IGroupBuyOrderListDao groupBuyOrderListDao;
    private final INotifyTaskDao notifyTaskDao;
    private final DCCService dccService;
    private final RedissonClient redissonClient;

    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String topic_team_success;

    @Override
    public MarketPayOrderEntity queryGroupBuyOrderRecordByOutTradeNo(String userId, String outTradeNo) {
        // 1. 构建查询参数 (PO)
        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userId)
                .outTradeNo(outTradeNo)
                .build();

        // 2. 调用 DAO 查询
        GroupBuyOrderList groupBuyOrderList =
                groupBuyOrderListDao.queryGroupBuyOrderRecordByOutTradeNo(groupBuyOrderListReq);

        // 3. 判空
        if (null == groupBuyOrderList) return null;

        // 4. 将 PO 转换为 Domain Entity
        return MarketPayOrderEntity.builder()
                .teamId(groupBuyOrderList.getTeamId())
                .orderId(groupBuyOrderList.getOrderId())
                .deductionPrice(groupBuyOrderList.getDeductionPrice())
                .tradeOrderStatus(TradeOrderStatusEnumVO.valueOf(groupBuyOrderList.getStatus()))
                .build();
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        // 1. 构建查询参数
        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(teamId);

        // 2. 查询数据库
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyProgress(groupBuyOrderReq);

        // 3. 判空
        if (null == groupBuyOrder) return null;

        // 4. 将 PO 转换为 Domain Value Object
        return GroupBuyProgressVO.builder()
                .targetCount(groupBuyOrder.getTargetCount())
                .lockCount(groupBuyOrder.getLockCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 500)
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyLockOrderAggregate groupBuyOrderAggregate) {
        // 1. 拆解聚合根
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();
        NotifyConfigVO notifyConfigVO = payDiscountEntity.getNotifyConfigVO();
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();

        // 2. 决策：新团还是旧团？
        String teamId = payActivityEntity.getTeamId();

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(payActivityEntity.getValidTime());

        if (StringUtils.isBlank(teamId)) {
            // [分支A] 开新团
            // 生成 8 位随机数字作为团ID
            teamId = RandomStringUtils.randomNumeric(8);

            // 构建团单 (GroupBuyOrder)
            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(startTime)
                    .validEndTime(endTime)
                    .notifyType(notifyConfigVO.getNotifyType().getCode())
                    .notifyUrl(notifyConfigVO.getNotifyUrl())
                    .build();

            // 插入新团记录
            groupBuyOrderDao.insert(groupBuyOrder);
        } else {
            // [分支B] 加入旧团
            // 利用 SQL update ... where lock_count < target 进行乐观锁更新
            int updateCount = groupBuyOrderDao.updateAddLockCount(teamId);
            if (1 != updateCount) {
                // 抛出 "拼团已满" 或 "活动结束" 异常
                throw new AppException(ResponseCode.E0005);
            }
        }

        // 3. 构建订单明细 (契约落库)
        String orderId = RandomStringUtils.randomNumeric(12);

        GroupBuyOrderList groupBuyOrderList = GroupBuyOrderList.builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(payActivityEntity.getStartTime())
                .endTime(payActivityEntity.getEndTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource())
                .channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                .bizId(payActivityEntity.getActivityId() + "_" + userEntity.getUserId() + "_" + (userTakeOrderCount + 1))
                .createTime(startTime) // 记录创建时间
                .updateTime(startTime) // 记录更新时间
                .build();

        try {
            // 插入订单明细
            groupBuyOrderListDao.insert(groupBuyOrderList);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突（如 bizId 重复），说明用户重复下单
            throw new AppException(ResponseCode.INDEX_EXCEPTION);
        }

        // 4. 返回结果
        return MarketPayOrderEntity.builder()
                .orderId(orderId)
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .tradeOrderStatus(TradeOrderStatusEnumVO.CREATE)
                .build();
    }

    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityByActivityId(Long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityByActivityId(activityId);
        return GroupBuyActivityEntity.builder()
                .activityId(groupBuyActivity.getActivityId())
                .activityName(groupBuyActivity.getActivityName())
                .discountId(groupBuyActivity.getDiscountId())
                .groupType(groupBuyActivity.getGroupType())
                .takeLimitCount(groupBuyActivity.getTakeLimitCount())
                .targetCount(groupBuyActivity.getTarget())
                .validTime(groupBuyActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(groupBuyActivity.getStatus()))
                .startTime(groupBuyActivity.getStartTime())
                .endTime(groupBuyActivity.getEndTime())
                .tagId(groupBuyActivity.getTagId())
                .tagScope(groupBuyActivity.getTagScope())
                .build();
    }

    @Override
    public Integer queryOrderCountByActivityIdAndUserId(Long activityId, String userId) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        return groupBuyOrderListDao.queryOrderCountByActivityId(groupBuyOrderListReq);
    }

    @Transactional(rollbackFor = Exception.class, timeout = 500)
    @Override
    public NotifyTaskEntity settlement(GroupBuyTeamSettlementAggregate aggregate) {

        // 1. 解包参数
        UserEntity userReq = aggregate.getUserEntity();
        GroupBuyTeamEntity teamReq = aggregate.getGroupBuyTeamEntity();
        NotifyConfigVO notifyConfigVO = teamReq.getNotifyConfigVO();
        TradePaySettlementEntity payReq = aggregate.getTradePaySettlementEntity();

        // 2. 更新个人订单状态 (幂等性防线)
        GroupBuyOrderList orderReq = new GroupBuyOrderList();
        orderReq.setUserId(userReq.getUserId());
        orderReq.setOutTradeNo(payReq.getOutTradeNo());
        orderReq.setOutTradeTime(payReq.getOutTradeTime());

        Integer updateOrderCount = groupBuyOrderListDao.updateOrderStatus2COMPLETE(orderReq);
        if (1 != updateOrderCount) {
            log.warn("交易结算-更新个人订单失败. userId:{} outTradeNo:{}", userReq.getUserId(), payReq.getOutTradeNo());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 3. 更新拼团进度
        int updateTeamCount = groupBuyOrderDao.AddCompleteCount(teamReq.getTeamId());
        if (1 != updateTeamCount) {
            log.error("交易结算-更新拼团进度失败. teamId:{}", teamReq.getTeamId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 4. 重新查询当前最新进度 (Double Check)
        Integer currentCompleteCount = groupBuyOrderDao.queryGroupBuyTeamCompleteCountByTeamId(teamReq.getTeamId());

        // 5. 判满逻辑：当前进度 >= 目标进度
        if (currentCompleteCount >= teamReq.getTargetCount()) {

            // 尝试更新团状态为 COMPLETE
            int updateStatusCount = groupBuyOrderDao.updateTeamStatus2COMPLETE(teamReq.getTeamId());

            if (1 == updateStatusCount) {
                log.info("交易结算-拼团成功撞线! teamId:{}", teamReq.getTeamId());

                // 5.1 查询所有团员单号
                List<String> outTradeNoList =
                        groupBuyOrderListDao.queryGroupBuyCompleteOrderOutTradeNoListByTeamId(teamReq.getTeamId());

                // 5.2 写入通知任务
                NotifyTask notifyTask = new NotifyTask();
                notifyTask.setActivityId(teamReq.getActivityId());
                notifyTask.setTeamId(teamReq.getTeamId());
                notifyTask.setNotifyType(notifyConfigVO.getNotifyType().getCode());
                notifyTask.setNotifyMQ(NotifyTypeEnumVO.MQ.equals(notifyConfigVO.getNotifyType()) ?
                        notifyConfigVO.getNotifyMQ() : null);
                notifyTask.setNotifyUrl(NotifyTypeEnumVO.HTTP.equals(notifyConfigVO.getNotifyType()) ?
                        notifyConfigVO.getNotifyUrl() : null);
                notifyTask.setNotifyCount(0);
                notifyTask.setNotifyStatus(0);
                notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                    put("teamId", teamReq.getTeamId());
                    put("outTradeNoList", outTradeNoList);
                }}));

                notifyTaskDao.insert(notifyTask);
                return NotifyTaskEntity.builder()
                        .teamId(notifyTask.getTeamId())
                        .notifyType(notifyTask.getNotifyType())
                        .notifyMQ(notifyTask.getNotifyMQ())
                        .notifyUrl(notifyTask.getNotifyUrl())
                        .notifyCount(notifyTask.getNotifyCount())
                        .parameterJson(notifyTask.getParameterJson())
                        .build();
            }
        }
        return null;
    }

    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return dccService.isSCBlackIntercept(source, channel);
    }

    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);
        return GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrder.getTeamId())
                .activityId(groupBuyOrder.getActivityId())
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .status(GroupBuyTeamStatusVO.valueOf(groupBuyOrder.getStatus()))
                .validStartTime(groupBuyOrder.getValidStartTime())
                .validEndTime(groupBuyOrder.getValidEndTime())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.valueOf(groupBuyOrder.getNotifyType()))
                        .notifyUrl(groupBuyOrder.getNotifyUrl())
                        .notifyMQ(topic_team_success)
                        .build())
                .build();
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build());
    }

    @Override
    public int updateNotifyTaskStatusSuccess(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusSuccess(teamId);
    }

    @Override
    public int updateNotifyTaskStatusError(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusError(teamId);
    }

    @Override
    public int updateNotifyTaskStatusRetry(String teamId) {
        return notifyTaskDao.updateNotifyTaskStatusRetry(teamId);
    }

    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer targetCount,
                                   Integer validTime) {
        long recoveryCount = redissonClient.getAtomicLong(recoveryTeamStockKey).get();
        redissonClient.getAtomicLong(recoveryTeamStockKey).get();

        RAtomicLong teamStock = redissonClient.getAtomicLong(teamStockKey);
        long occupy = teamStock.incrementAndGet() + 1;
        if (occupy > targetCount + recoveryCount) {
            teamStock.set(targetCount);
            return false;
        }
        // 1. 拼装占位 Key (例如: group_buy_stock_1001_5)
        String lockKey = teamStockKey + Constants.UNDERLINE + occupy;

        // 2. 利用 SETNX 占座
        RBucket<String> bucket = redissonClient.getBucket(lockKey);

        // 3. 尝试写入 (Value 写什么不重要，只要不为空即可)
        boolean lock = bucket.setIfAbsent("occupied", Duration.ofMinutes(validTime + 60));

        if (!lock) {
            log.info("组队库存疑似重复占用或并发冲突 {}", lockKey);
        }
        return lock;
    }

    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        if (StringUtils.isBlank(recoveryTeamStockKey)) return;
        redissonClient.getAtomicLong(recoveryTeamStockKey).incrementAndGet();
    }

    @Override
    @Transactional(timeout = 5000)
    public void unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        GroupBuyProgressVO groupBuyProgress = groupBuyRefundAggregate.getGroupBuyProgress();
        TradeRefundOrderEntity tradeRefundOrderEntity = groupBuyRefundAggregate.getTradeRefundOrderEntity();

        // 1. 更新个人单
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        groupBuyOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());
        int orderUpdateCount = groupBuyOrderListDao.unpaid2Refund(groupBuyOrderListReq);
        if (1 != orderUpdateCount){
            log.error("逆向流程，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 2. 更新团单
        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        groupBuyOrderReq.setLockCount(groupBuyProgress.getLockCount());
        int teamUpdateCount = groupBuyOrderDao.unpaid2Refund(groupBuyOrderReq);
        if (1 != teamUpdateCount) {
            log.error("逆向流程，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }
    }
}
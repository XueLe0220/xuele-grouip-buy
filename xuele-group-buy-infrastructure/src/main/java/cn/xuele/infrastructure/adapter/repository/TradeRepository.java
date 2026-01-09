package cn.xuele.infrastructure.adapter.repository;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.xuele.infrastructure.dao.IGroupBuyOrderDao;
import cn.xuele.infrastructure.dao.IGroupBuyOrderListDao;
import cn.xuele.infrastructure.dao.po.GroupBuyOrder;
import cn.xuele.infrastructure.dao.po.GroupBuyOrderList;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

    private final IGroupBuyOrderDao groupBuyOrderDao;
    private final IGroupBuyOrderListDao groupBuyOrderListDao;

    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
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
                .orderId(groupBuyOrderList.getOrderId())
                .deductionPrice(groupBuyOrderList.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(groupBuyOrderList.getStatus()))
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
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {

        // 1. 拆解聚合根：获取内部实体
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();

        // 2. 决策：是“开新团”还是“加入旧团”？
        String teamId = payActivityEntity.getTeamId();

        if (StringUtils.isBlank(teamId)) {
            // ==================== 分支 A: 开新团 ====================

            teamId = RandomStringUtils.randomNumeric(8);

            // 计算支付金额 (CR优化：防止 payPrice 逻辑错误，这里建议明确计算逻辑)
            // 假设 payPrice = original - deduction
            BigDecimal payPrice = payDiscountEntity.getOriginalPrice().subtract(payDiscountEntity.getDeductionPrice());

            // 构建拼团主单 PO
            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payPrice) // CR修复：使用计算后的实付金额
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0) // 新团完成数为0
                    .lockCount(1)     // 锁单数为1 (自己)
                    .build();

            // 插入拼团主表
            groupBuyOrderDao.insert(groupBuyOrder);
        } else {
            // ==================== 分支 B: 加入旧团 ====================

            // 尝试更新锁单数量 (利用数据库行锁防止超卖)
            // SQL 逻辑: UPDATE ... SET lock_count = lock_count + 1 WHERE team_id = ? AND (lock + complete) < target
            int updateAddTargetCount = groupBuyOrderDao.updateAddLockCount(teamId);

            // 更新失败意味着：要么团不存在，要么团已满
            if (1 != updateAddTargetCount) {
                throw new AppException(ResponseCode.E0005); // 锁单失败(满员)
            }
        }

        // 3. 构建用户订单明细 (落库契约)
    String orderId = RandomStringUtils.randomNumeric(12);

        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
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
                // 状态：初始创建(0)
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                .build();

        try {
            // 插入订单明细表
            // 利用数据库唯一索引 (order_id 或 out_trade_no) 进行最终的兜底防重
            groupBuyOrderListDao.insert(groupBuyOrderListReq);
        } catch (DuplicateKeyException e) {
            // 捕获数据库的主键冲突/唯一索引冲突异常，转换为业务异常
            throw new AppException(ResponseCode.INDEX_EXCEPTION);
        }

        // 4. 返回包含 orderId 的结果实体
        return MarketPayOrderEntity.builder()
                .orderId(orderId)
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }
}
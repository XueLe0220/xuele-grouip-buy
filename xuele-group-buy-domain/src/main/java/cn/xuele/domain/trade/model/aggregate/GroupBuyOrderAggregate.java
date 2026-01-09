package cn.xuele.domain.trade.model.aggregate;

import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团订单聚合根 (Aggregate Root)
 * <p>
 * 领域定义：
 * 它是整个拼团交易上下文的“总指挥”和“数据容器”。
 * 它将分散的“人(User)”、“事(Activity)”、“钱(Discount)”聚合在一起，
 * 形成一个完整的、有一致性保证的业务对象。
 * <p>
 * 核心作用：
 * 1. 保证数据一致性：锁单操作必须同时具备这三要素，缺一不可。
 * 2. 事务边界：对聚合根的操作，通常对应一个完整的数据库事务。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 15:14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {

    /**
     * 用户实体
     * 代表“谁”在发起这个聚合操作。
     */
    private UserEntity userEntity;

    /**
     * 支付活动实体
     * 代表“在什么规则下”进行的交易。
     * (包含 teamId, activityId 等上下文)
     */
    private PayActivityEntity payActivityEntity;

    /**
     * 支付优惠实体
     * 代表“具体的交易金额和商品”。
     * (包含 outTradeNo, price 等)
     */
    private PayDiscountEntity payDiscountEntity;

}
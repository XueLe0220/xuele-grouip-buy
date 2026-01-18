package cn.xuele.domain.trade.model.aggregate;

import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团订单聚合根 (Aggregate Root)。
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

    /** 用户实体 */
    private UserEntity userEntity;

    /** 支付活动实体 */
    private PayActivityEntity payActivityEntity;

    /** 支付优惠实体 */
    private PayDiscountEntity payDiscountEntity;

    /** 已参与拼团量 */
    private Integer userTakeOrderCount;

}
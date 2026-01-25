package cn.xuele.domain.trade.model.aggregate;

import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团退单聚合根
 * <p>
 * 用于在退单过程中，聚合【订单实体】与【拼团进度】数据，保证数据操作的完整性。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:47
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyRefundAggregate {
    /**
     * 交易退单实体
     */
    private TradeRefundOrderEntity tradeRefundOrderEntity;

    /**
     * 拼团进度对象 (用于处理锁单数变更)
     */
    private GroupBuyProgressVO groupBuyProgress;

    private GroupBuyTeamStatusVO groupBuyTeamStatusVO;

    /**
     * 静态工厂：构建【未支付取消】场景的聚合对象
     *
     * @param tradeRefundOrderEntity 退单实体
     * @param lockCount              锁单变更数量 (通常为负数，表示释放)
     */
    public static GroupBuyRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                      int lockCount) {

        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        // 构建进度对象，仅关注锁单数量的变化
        groupBuyRefundAggregate.setGroupBuyProgress(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .build());
        return groupBuyRefundAggregate;

    }

    public static GroupBuyRefundAggregate buildPaidUnformed2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                            int lockCont,
                                                                            int completeCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgress(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCont)
                        .completeCount(completeCount)
                        .build()
        );
        return groupBuyRefundAggregate;

    }

    public static GroupBuyRefundAggregate buildPaidFormed2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                          int lockCount,
                                                                          int completeCount,
                                                                          GroupBuyTeamStatusVO status) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = buildPaidUnformed2RefundAggregate(tradeRefundOrderEntity,
                lockCount, completeCount);
        groupBuyRefundAggregate.setGroupBuyTeamStatusVO(status);
        return groupBuyRefundAggregate;
    }

}
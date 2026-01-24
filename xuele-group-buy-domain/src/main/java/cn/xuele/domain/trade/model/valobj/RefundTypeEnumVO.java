package cn.xuele.domain.trade.model.valobj;

import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 退单类型枚举
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 15:40
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum RefundTypeEnumVO {
    UNPAID_UNLOCK("unpaid_unlock", "paidFormed2RefundStrategy", "未支付，未成团") {
        @Override
        public boolean matches(GroupBuyTeamStatusVO groupBuyTeamStatusVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GroupBuyTeamStatusVO.PROGRESS.equals(groupBuyTeamStatusVO) && TradeOrderStatusEnumVO.CREATE.equals(tradeOrderStatusEnumVO);
        }
    },

    PAID_UNFORMED("paid_unformed", "paidUnformed2RefundStrategy", "已支付，未成团") {
        @Override
        public boolean matches(GroupBuyTeamStatusVO groupBuyTeamStatusVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GroupBuyTeamStatusVO.PROGRESS.equals(groupBuyTeamStatusVO) && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    },

    PAID_FORMED("paid_formed", "paidTeam2RefundStrategy", "已支付，已成团"){

        @Override
        public boolean matches(GroupBuyTeamStatusVO groupBuyTeamStatusVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GroupBuyTeamStatusVO.COMPLETE.equals(groupBuyTeamStatusVO) && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    }

    ;

    private String code;
    private String strategy;
    private String info;

    public abstract boolean matches(GroupBuyTeamStatusVO groupBuyTeamStatusVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO);

    public static RefundTypeEnumVO getRefundStrategy(GroupBuyTeamStatusVO groupBuyTeamStatusVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO){
        return Arrays.stream(values())
                .filter(refundType-> refundType.matches(groupBuyTeamStatusVO, tradeOrderStatusEnumVO))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的退款状态组合: groupBuyTeamStatus=" + groupBuyTeamStatusVO + ", tradeOrderStatus=" + tradeOrderStatusEnumVO));
    }

    public static RefundTypeEnumVO valueOf(Integer code) {
        return switch (code) {
            case 1 -> UNPAID_UNLOCK;
            case 2 -> PAID_UNFORMED;
            case 3 -> PAID_FORMED;
            default -> throw new RuntimeException("退单类型枚举值不存在: " + code);
        };
    }
}

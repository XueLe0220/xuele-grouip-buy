package cn.xuele.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 任务类型枚举
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/24 15:55
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum TaskNotifyCategoryEnumVO {
    TRADE_SETTLEMENT("trade_settlement","交易结算"),
    TRADE_UNPAID2REFUND("trade_unpaid2refund","交易退单-未支付&未成团"),
    TRADE_PAID_UNFORMED2REFUND("trade_paid_unformed2refund","交易退单-已支付&未成团"),
    TRADE_PAID_FORMED2REFUND("trade_paid_formed2refund","交易退单-已支付&已成团"),

    ;

    private String code;
    private String info;
}

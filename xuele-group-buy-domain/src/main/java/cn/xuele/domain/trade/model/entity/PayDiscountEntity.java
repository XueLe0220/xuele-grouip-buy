package cn.xuele.domain.trade.model.entity;

import cn.xuele.domain.trade.model.valobj.NotifyConfigVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付优惠实体 (兼具商品信息与计价上下文)
 * <p>
 * 领域定义：
 * 在交易锁单阶段，该实体负责传递“买什么(商品)”以及“怎么算钱(价格/优惠)”的核心信息。
 * 同时携带外部交易单号，用于幂等性控制。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayDiscountEntity {

    /** 渠道 */
    private String source;

    /** 来源 */
    private String channel;

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 折扣金额 */
    private BigDecimal deductionPrice;

    /** 应付金额 */
    private BigDecimal payableAmount;

    /** 外部交易单号 */
    private String outTradeNo;


    /** 回调配置 */
    private NotifyConfigVO notifyConfigVO;


}
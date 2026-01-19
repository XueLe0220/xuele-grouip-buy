package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 交易结算规则入参实体 (Command)
 * <p>
 * 领域定义：规则引擎的“输入指令包”。
 * 作用：承载所有用于【结算校验】的原始数据，驱动规则过滤器链条执行。
 * 对应规则：
 * 1. 渠道黑名单校验 -> 依赖 source/channel
 * 2. 外部单号校验 -> 依赖 outTradeNo
 * 3. 交易时间校验 -> 依赖 outTradeTime
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSettlementRuleCommandEntity {

    /**
     * 渠道标识 (校验核心)
     * <p>
     * 规则1：用于【黑名单校验】。
     * 比如某些渠道只有“查询”权限，没有“写/结算”权限，需在此拦截。
     */
    private String source;

    /**
     * 来源标识 (校验核心)
     * <p>
     * 规则1：细分来源校验。
     */
    private String channel;

    /**
     * 用户ID
     * <p>
     * 作用：全链路日志追踪 (Trace)，以及可能的“用户维度的风控规则”。
     */
    private String userId;

    /**
     * 外部交易单号 (关键索引)
     * <p>
     * 规则2：用于【反查订单】。
     * 规则引擎需要拿着这个号去数据库查：这笔单子是否存在？是否已经结算过？
     */
    private String outTradeNo;

    /**
     * 外部交易时间
     * <p>
     * 规则3：用于【时效性校验】。
     * 防止处理过老的脏数据
     */
    private LocalDateTime outTradeTime;

}
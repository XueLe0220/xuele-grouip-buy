package cn.xuele.domain.trade.model.aggregate;

import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团交易结算聚合根
 * <p>
 * 领域定义：
 * 它是【拼团结算】业务场景下的数据一致性边界。
 * 封装了“用户”、“拼团进度”、“支付单”三个维度的核心对象，用于在 Repository 层执行原子性事务更新。
 * <p>
 * 作用：
 * 1. 完整性：保证结算操作所需的所有数据都在一个包里。
 * 2. 原子性：Repository 接收此对象后，将在一个 DB 事务中同时更新 GroupBuyTeam(进度) 和 Order(状态)。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamSettlementAggregate {

    /**
     * 交易主体 (用户)
     * <p>
     * 代表“谁”完成了这次支付。
     * 用于记录日志、发送通知或风控校验。
     */
    private UserEntity userEntity;

    /**
     * 核心领域对象 (拼团组队信息)
     * <p>
     * 代表“被修改的目标”。
     * 结算服务会根据 tradePaySettlementEntity 的支付结果，来更新本对象的 completeCount 和 status。
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;

    /**
     * 结算凭证 (交易支付单)
     * <p>
     * 代表“驱动本次结算的事件源”。
     * 包含外部流水号、支付金额、支付时间等关键核销信息。
     */
    private TradePaySettlementEntity tradePaySettlementEntity;
}
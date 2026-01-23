package cn.xuele.domain.trade.service.refund;

import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundCommandEntity;
import cn.xuele.domain.trade.model.entity.TradeRefundOrderEntity;
import cn.xuele.domain.trade.model.valobj.RefundTypeEnumVO;
import cn.xuele.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.xuele.domain.trade.service.refund.business.IRefundStrategy;
import cn.xuele.types.enums.GroupBuyTeamStatusVO;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 交易逆向流程服务
 * <p>
 * 核心职责：处理各类退单请求（未支付取消、拼团失败退款等），
 * 通过策略模式路由到具体的执行逻辑。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 14:27
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class TradeRefundOrderService implements ITradeRefundOrderService {

    private final ITradeRepository repository;
    private final Map<String, IRefundStrategy> refundStrategyMap;

    @Override
    public TradeRefundBehaviorEntity refund(TradeRefundCommandEntity tradeRefundCommandEntity) {
        log.info("逆向流程-开始退单 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 1. 查询订单信息
        MarketPayOrderEntity marketPayOrderEntity = repository.queryGroupBuyOrderRecordByOutTradeNo(
                tradeRefundCommandEntity.getUserId(),
                tradeRefundCommandEntity.getOutTradeNo()
        );

        // 2. 基础校验 (防止空指针)
        if (null == marketPayOrderEntity) {
            log.warn("逆向流程-订单不存在 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
            throw new AppException(ResponseCode.E0002.getCode(), "订单不存在");
        }

        TradeOrderStatusEnumVO tradeOrderStatus = marketPayOrderEntity.getTradeOrderStatus();
        String teamId = marketPayOrderEntity.getTeamId();
        String orderId = marketPayOrderEntity.getOrderId();

        // 3. 幂等性校验 (如果订单已关闭/已退款，直接返回重复标识)
        if (TradeOrderStatusEnumVO.CLOSE.equals(tradeOrderStatus)) {
            log.info("逆向流程-拦截重复退单 userId:{} orderId:{}", tradeRefundCommandEntity.getUserId(), orderId);
            return TradeRefundBehaviorEntity.builder()
                    .userId(tradeRefundCommandEntity.getUserId())
                    .orderId(orderId)
                    .teamId(teamId)
                    .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.REPEAT)
                    .build();
        }

        // 4. 获取全局拼团状态 (用于决策退款策略)
        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(teamId);
        GroupBuyTeamStatusVO groupBuyTeamStatus = groupBuyTeamEntity.getStatus();

        // 5. 策略路由 & 执行退单
        RefundTypeEnumVO refundType = RefundTypeEnumVO.getRefundStrategy(groupBuyTeamStatus, tradeOrderStatus);
        IRefundStrategy refundStrategy = refundStrategyMap.get(refundType.getStrategy());

        refundStrategy.refund(TradeRefundOrderEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(orderId)
                .teamId(teamId)
                .build());

        // 6. 返回成功结果
        return TradeRefundBehaviorEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(orderId)
                .teamId(teamId)
                .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.SUCCESS)
                .build();
    }
}
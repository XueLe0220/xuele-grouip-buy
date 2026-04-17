package cn.xuele.trigger.http;

import cn.xuele.api.IMarketTradeService;
import cn.xuele.api.dto.LockMarketPayOrderRequestDTO;
import cn.xuele.api.dto.LockMarketPayOrderResponseDTO;
import cn.xuele.api.dto.SettlementMarketPayOrderRequestDTO;
import cn.xuele.api.dto.SettlementMarketPayOrderResponseDTO;
import cn.xuele.api.response.Response;
import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.IIndexGroupBuyMarketService;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.TradePaySettlementEntity;
import cn.xuele.domain.trade.model.entity.TradeSettlementEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.domain.trade.model.valobj.NotifyConfigVO;
import cn.xuele.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.xuele.domain.trade.service.ITradeLockOrderService;
import cn.xuele.domain.trade.service.ITradeSettlementOrderService;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;

/**
 * 营销交易 HTTP 接口
 * <p>
 * 职责：作为营销(Activity)与交易(Trade)领域的业务编排层，负责处理拼团锁单流程。
 *
 * @author XueLe
 * @since 2026/01/08
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/trade/")
@RequiredArgsConstructor
public class MarketTradeController implements IMarketTradeService {

    /** 营销领域服务：负责试算价格、校验活动规则 */
    private final IIndexGroupBuyMarketService indexGroupBuyMarketService;

    /** 交易领域服务：负责订单创建、状态流转、预占库存/名额 */
    private final ITradeLockOrderService tradeLockOrderService;

    private final ITradeSettlementOrderService tradeSettlementOrderService;


    /**
     * 营销拼团锁单
     * 流程：参数校验 -> 幂等检查 -> 拼团容量校验 -> 营销试算 -> 执行锁单
     */
    @RequestMapping(value = "")
    @Override
    public Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO) {
        try {
            log.info("营销交易锁单-开始 userId:{} outTradeNo:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo());
            LockMarketPayOrderRequestDTO.NotifyConfigVO notifyConfigVO = requestDTO.getNotifyConfigVO();
            // 1. 基础参数非空校验
            if (StringUtils.isAnyBlank(requestDTO.getUserId(), requestDTO.getGoodsId()) || null == requestDTO.getActivityId() || ("HTTP".equals(notifyConfigVO.getNotifyType()) && StringUtils.isBlank(notifyConfigVO.getNotifyUrl()))) {
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 2. 幂等性检查：防止重复提交，若已存在未支付订单则直接返回
            MarketPayOrderEntity existOrder = tradeLockOrderService.queryNoPayMarketPayOrderByOutTradeNo(
                    requestDTO.getUserId(), requestDTO.getOutTradeNo());
            if (null != existOrder) {
                return buildSuccessResponse(existOrder);
            }

            // 3. 拼团容量预校验：针对“加入存量团”，检查目标团是否已满
            if (StringUtils.isNotBlank(requestDTO.getTeamId())) {
                GroupBuyProgressVO progressVO = tradeLockOrderService.queryGroupBuyProgress(requestDTO.getTeamId());
                if (null != progressVO && Objects.equals(progressVO.getTargetCount(), progressVO.getLockCount())) {
                    return Response.<LockMarketPayOrderResponseDTO>builder()
                            .code(ResponseCode.E0006.getCode())
                            .info(ResponseCode.E0006.getInfo())
                            .build();
                }
            }

            // 4. 营销试算：计算最终支付价格并核实用户参与资格（人群过滤）
            TrialBalanceEntity trialBalance = indexGroupBuyMarketService.indexMarketTrial(
                    MarketProductEntity.builder()
                            .userId(requestDTO.getUserId())
                            .source(requestDTO.getSource())
                            .channel(requestDTO.getChannel())
                            .goodsId(requestDTO.getGoodsId())
                            .activityId(requestDTO.getActivityId())
                            .build());

            if (!trialBalance.getIsEnable() || !trialBalance.getIsVisible()) {
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.E0007.getCode())
                        .info(ResponseCode.E0007.getInfo())
                        .build();
            }

            // 5. 执行锁单：聚合营销与交易信息，完成订单落库
            GroupBuyActivityDiscountVO discountVO = trialBalance.getGroupBuyActivityDiscountVO();
            MarketPayOrderEntity newOrder = tradeLockOrderService.lockMarketPayOrder(
                    UserEntity.builder().userId(requestDTO.getUserId()).build(),
                    PayActivityEntity.builder()
                            .teamId(requestDTO.getTeamId())
                            .activityId(requestDTO.getActivityId())
                            .activityName(discountVO.getActivityName())
                            .startTime(discountVO.getStartTime())
                            .endTime(discountVO.getEndTime())
                            .targetCount(discountVO.getTarget())
                            .validTime(discountVO.getValidTime())
                            .build(),
                    PayDiscountEntity.builder()
                            .source(requestDTO.getSource())
                            .channel(requestDTO.getChannel())
                            .goodsId(requestDTO.getGoodsId())
                            .goodsName(trialBalance.getGoodsName())
                            .originalPrice(trialBalance.getOriginalPrice())
                            .deductionPrice(trialBalance.getDeductionPrice())
                            .payPrice(trialBalance.getPayPrice())
                            .outTradeNo(requestDTO.getOutTradeNo())
                            .notifyConfigVO(
                                    NotifyConfigVO.builder()
                                            .notifyType(NotifyTypeEnumVO.valueOf(notifyConfigVO.getNotifyType()))
                                            .notifyMQ(notifyConfigVO.getNotifyMQ())
                                            .notifyUrl(notifyConfigVO.getNotifyUrl())
                                            .build()
                            )
                            .build());

            log.info("营销交易锁单-成功 userId:{} orderId:{}", requestDTO.getUserId(), newOrder.getOrderId());
            return buildSuccessResponse(newOrder);

        } catch (AppException e) {
            log.error("营销交易锁单业务异常 userId:{}", requestDTO.getUserId(), e);
            return Response.<LockMarketPayOrderResponseDTO>builder().code(e.getCode()).info(e.getInfo()).build();
        } catch (Exception e) {
            log.error("营销交易锁单系统未知异常 userId:{}", requestDTO.getUserId(), e);
            return Response.<LockMarketPayOrderResponseDTO>builder().code(ResponseCode.UN_ERROR.getCode()).info(ResponseCode.UN_ERROR.getInfo()).build();
        }
    }


    @RequestMapping(value = "settlement_market_pay_order", method = RequestMethod.POST)
    @Override
    public Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(@RequestBody SettlementMarketPayOrderRequestDTO requestDTO){
        try {
            log.info("营销交易组队结算开始:{} outTradeNo:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo());

            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getOutTradeNo()) || null == requestDTO.getOutTradeTime()) {
                return Response.<SettlementMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 1. 结算服务
            TradeSettlementEntity tradeSettlementEntity = tradeSettlementOrderService.settlement(TradePaySettlementEntity.builder()
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .userId(requestDTO.getUserId())
                    .outTradeNo(requestDTO.getOutTradeNo())
                    .outTradeTime(requestDTO.getOutTradeTime())
                    .build());

            SettlementMarketPayOrderResponseDTO responseDTO = SettlementMarketPayOrderResponseDTO.builder()
                    .userId(tradeSettlementEntity.getUserId())
                    .teamId(tradeSettlementEntity.getTeamId())
                    .activityId(tradeSettlementEntity.getActivityId())
                    .outTradeNo(tradeSettlementEntity.getOutTradeNo())
                    .build();

            // 返回结果
            Response<SettlementMarketPayOrderResponseDTO> response = Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();

            log.info("营销交易组队结算完成:{} outTradeNo:{} response:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo(), JSON.toJSONString(response));

            return response;
        } catch (AppException e) {
            log.error("营销交易组队结算异常:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("营销交易组队结算失败:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 构造通用的成功返回对象
     */
    private Response<LockMarketPayOrderResponseDTO> buildSuccessResponse(MarketPayOrderEntity order) {
        // 返回结果
        return Response.<LockMarketPayOrderResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(LockMarketPayOrderResponseDTO.builder()
                        .orderId(order.getOrderId())
                        .originalPrice(order.getOriginalPrice())
                        .deductionPrice(order.getDeductionPrice())
                        .payPrice(order.getPayPrice())
                        .tradeOrderStatus(order.getTradeOrderStatusEnumVO().getCode())
                        .teamId(order.getTeamId())
                        .build())
                .build();
    }
}
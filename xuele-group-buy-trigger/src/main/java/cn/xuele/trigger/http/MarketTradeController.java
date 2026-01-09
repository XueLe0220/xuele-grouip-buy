package cn.xuele.trigger.http;

import cn.xuele.api.IMarketTradeService;
import cn.xuele.api.dto.LockMarketPayOrderRequestDTO;
import cn.xuele.api.dto.LockMarketPayOrderResponseDTO;
import cn.xuele.api.response.Response;
import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.IIndexGroupBuyMarketService;
import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.xuele.domain.trade.service.ITradeOrderService;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 营销交易 HTTP 触发器
 * <p>
 * 职责：作为“总指挥”，协调 Activity(算价) 和 Trade(锁单) 两个领域服务，
 * 将前端的“贫血”请求转换为后端“充血”的领域操作。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 23:56
 */
@Slf4j
@RestController()
@CrossOrigin("*") // 允许跨域
@RequestMapping("/api/v1/gbm/trade/")
@RequiredArgsConstructor
public class MarketTradeController implements IMarketTradeService {

    /** 注入活动领域服务：用于计算价格、获取活动规则 */
    private final IIndexGroupBuyMarketService indexGroupBuyMarketService;
    /** 注入交易领域服务：用于执行锁单、落库 */
    private final ITradeOrderService tradeOrderService;

    @Override
    public Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO) {
        try {
            String userId = requestDTO.getUserId();
            String source = requestDTO.getSource();
            String channel = requestDTO.getChannel();
            String goodsId = requestDTO.getGoodsId();
            Long activityId = requestDTO.getActivityId();
            String outTradeNo = requestDTO.getOutTradeNo();
            String teamId = requestDTO.getTeamId();

            log.info("营销交易锁单-开始:{} param:{}", userId, JSON.toJSONString(requestDTO));

            if (StringUtils.isBlank(userId) || StringUtils.isBlank(goodsId)
                    || null == activityId) {
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 2. 幂等性检查
            MarketPayOrderEntity existOrder = tradeOrderService.queryNoPayMarketPayOrderByOutTradeNo(userId, outTradeNo);
            if (null != existOrder) {
                log.info("交易锁单-返回已存在订单:{} orderId:{}", userId, existOrder.getOrderId());
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .data(LockMarketPayOrderResponseDTO.builder()
                                .orderId(existOrder.getOrderId())
                                .deductionPrice(existOrder.getDeductionPrice())
                                .tradeOrderStatus(existOrder.getTradeOrderStatusEnumVO().getCode())
                                .build())
                        .build();
            }

            // 3. 拼团容量预校验
            // 如果是加入旧团，必须先检查是否满员。虽然数据库有防超卖，但在这里拦截能减少数据库压力。
            if (StringUtils.isNotBlank(teamId)) {
                GroupBuyProgressVO progressVO = tradeOrderService.queryGroupBuyProgress(teamId);
                if (null != progressVO && Objects.equals(progressVO.getTargetCount(), progressVO.getLockCount())) {
                    log.info("交易锁单拦截-拼单目标已达成:{} teamId:{}", userId, teamId);
                    return Response.<LockMarketPayOrderResponseDTO>builder()
                            .code(ResponseCode.E0006.getCode())
                            .info(ResponseCode.E0006.getInfo())
                            .build();
                }
            }

            // 4. 营销试算
            TrialBalanceEntity trialBalance = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                    .userId(userId)
                    .source(source)
                    .channel(channel)
                    .goodsId(goodsId)
                    .activityId(activityId)
                    .build());

            GroupBuyActivityDiscountVO discountVO = trialBalance.getGroupBuyActivityDiscountVO();

            // 5. 执行锁单
            // 组装完整的领域实体，调用交易服务
            MarketPayOrderEntity newOrder = tradeOrderService.lockMarketPayOrder(
                    UserEntity.builder().userId(userId).build(),
                    PayActivityEntity.builder()
                            .teamId(teamId)
                            .activityId(activityId)
                            .activityName(discountVO.getActivityName())
                            .startTime(discountVO.getStartTime())
                            .endTime(discountVO.getEndTime())
                            .targetCount(discountVO.getTarget())
                            .build(),
                    PayDiscountEntity.builder()
                            .source(source)
                            .channel(channel)
                            .goodsId(goodsId)
                            .goodsName(trialBalance.getGoodsName())
                            .originalPrice(trialBalance.getOriginalPrice())
                            .deductionPrice(trialBalance.getDeductionPrice())
                            .outTradeNo(outTradeNo)
                            .build());

            log.info("交易锁单-成功:{} orderId:{}", userId, newOrder.getOrderId());

            // 6. 封装响应
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(LockMarketPayOrderResponseDTO.builder()
                            .orderId(newOrder.getOrderId())
                            .deductionPrice(newOrder.getDeductionPrice())
                            .tradeOrderStatus(newOrder.getTradeOrderStatusEnumVO().getCode())
                            .build())
                    .build();

        } catch (AppException e) {
            log.error("营销交易锁单业务异常:{}", requestDTO.getUserId(), e);
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("营销交易锁单系统异常:{}", requestDTO.getUserId(), e);
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
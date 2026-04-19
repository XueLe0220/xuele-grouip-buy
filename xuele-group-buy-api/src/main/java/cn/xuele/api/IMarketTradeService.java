package cn.xuele.api;

import cn.xuele.api.dto.LockMarketPayOrderRequestDTO;
import cn.xuele.api.dto.LockMarketPayOrderResponseDTO;
import cn.xuele.api.dto.RefundMarketPayOrderRequestDTO;
import cn.xuele.api.dto.RefundMarketPayOrderResponseDTO;
import cn.xuele.api.dto.SettlementMarketPayOrderRequestDTO;
import cn.xuele.api.dto.SettlementMarketPayOrderResponseDTO;
import cn.xuele.api.response.Response;

/**
 * 营销交易服务接口 (RPC / HTTP 接口定义)
 * <p>
 * 职责：
 * 对外暴露拼团交易的核心能力。它是最外层的 API 契约，通常由 Controller 或 RPC Provider 实现。
 * 负责接收外部 DTO，转换为内部领域对象，并处理全局异常捕获和统一响应封装。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 23:52
 */
public interface IMarketTradeService {


    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销结算
     *
     * @param requestDTO 结算商品信息
     * @return 结算结果信息
     */
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder( SettlementMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销拼团退单
     *
     * @param requestDTO 退单请求信息
     * @return 退单结果信息
     */
    Response<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(RefundMarketPayOrderRequestDTO requestDTO);

}
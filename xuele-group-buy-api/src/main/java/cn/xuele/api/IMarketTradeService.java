package cn.xuele.api;

import cn.xuele.api.dto.LockMarketPayOrderRequestDTO;
import cn.xuele.api.dto.LockMarketPayOrderResponseDTO;
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
     * 营销拼团锁单接口
     * <p>
     * 场景：用户在客户端点击“立即拼单”或“去支付”时调用。
     * 逻辑：
     * 1. 校验活动是否有效、商品是否存在。
     * 2. 执行锁单逻辑（占用库存/坑位）。
     * 3. 返回订单号，前端据此唤起收银台。
     *
     * @param lockMarketPayOrderRequestDTO 锁单请求参数 (用户ID、活动ID、商品ID等)
     * @return 统一响应对象，包含订单ID、实付金额、订单状态
     */
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder( SettlementMarketPayOrderRequestDTO requestDTO);
}
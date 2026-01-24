package cn.xuele.domain.trade.service;

import cn.xuele.domain.trade.model.entity.MarketPayOrderEntity;
import cn.xuele.domain.trade.model.entity.PayActivityEntity;
import cn.xuele.domain.trade.model.entity.PayDiscountEntity;
import cn.xuele.domain.trade.model.entity.UserEntity;
import cn.xuele.domain.trade.model.valobj.GroupBuyProgressVO;

/**
 * 交易订单领域服务接口
 * <p>
 * 核心职责：负责拼团业务中“交易”维度的核心逻辑，不包含具体支付渠道（微信/支付宝）的实现，
 * 而是专注于订单的生成、状态流转和资源锁定（锁单）。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:04
 */
public interface ITradeLockOrderService {

    /**
     * 查询未支付的营销订单（防重/幂等性检查）
     * <p>
     * 业务场景：用户点击“支付”或“去拼单”时，先检查是否存在一笔基于当前 outTradeNo 的未支付订单。
     * 如果存在，直接返回该订单继续支付，避免重复锁单扣减库存。
     *
     * @param userId     用户ID
     * @param outTradeNo 外部交易单号（由前端或上层生成，用于保证幂等性）
     * @return MarketPayOrderEntity 存在的未支付订单实体，不存在则返回 null
     */
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    /**
     * 查询拼团进度
     * <p>
     * 业务场景：在锁单前，通过 teamId 查询当前团是否已满、是否已结束。
     * 这是防止“超员”的第一道业务校验。
     *
     * @param teamId 拼单组队ID
     * @return GroupBuyProgressVO 拼团进度值对象（包含目标人数、已完成人数、锁单人数）
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 锁单（核心业务）
     * <p>
     * 业务场景：用户发起拼单（开团或参团），系统进行资源预占。
     * 执行逻辑：
     * 1. 校验活动有效性、库存、拼团进度。
     * 2. 扣减库存/占用坑位 (group_buy_order.lock_count + 1)。
     * 3. 生成待支付订单 (group_buy_order_list)。
     *
     * @param userEntity        用户实体（包含用户基础信息）
     * @param payActivityEntity 支付活动实体（包含活动规则、商品信息）
     * @param payDiscountEntity 支付优惠实体（包含折扣信息，计算最终价格）
     * @return MarketPayOrderEntity 锁单成功后生成的订单实体
     */
    MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;
}
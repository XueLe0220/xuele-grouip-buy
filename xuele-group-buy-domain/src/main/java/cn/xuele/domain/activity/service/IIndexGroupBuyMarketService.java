package cn.xuele.domain.activity.service;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;

/**
 * 首页营销试算服务接口
 * <p>
 * 职责描述：
 * 提供给应用层(App)的统一入口，用于计算用户在特定商品上的优惠情况。
 * 核心功能是完成“商品列表页”或“商品详情页”的价格试算，不涉及下单锁库。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 12:53
 */
public interface IIndexGroupBuyMarketService {

    /**
     * 首页营销试算
     *
     * @param marketProductEntity 试算入参（包含用户ID、商品ID、渠道等上下文）
     * @return 试算结果（包含优惠后金额、是否可见、是否可买等状态）
     * @throws Exception 试算过程中可能抛出的业务异常或系统异常
     */
    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;
}
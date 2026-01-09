package cn.xuele.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营销产品实体（试算服务标准入参）
 * <p>
 * 描述：封装了“谁（User）”在“什么渠道（Channel/Source）”购买“什么商品（Goods）”的上下文信息。
 * 该实体作为领域服务 {@code IndexGroupBuyMarketService} 的统一输入参数。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketProductEntity {

    /** 活动ID */
    private Long activityId;

    /** 用户ID */
    private String userId;

    /** 商品ID */
    private String goodsId;

    /** 渠道 */
    private String source;

    /** 来源/媒介 */
    private String channel;

}
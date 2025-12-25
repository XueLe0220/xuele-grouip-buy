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

    /** * 用户ID
     * 作用：
     * 1. 身份识别：判断该用户是否已参团、是否达到限购次数。
     * 2. 人群标签：通过 userId 查询用户画像，判断是否命中特定人群规则（如：新人专享）。
     */
    private String userId;

    /** * 商品ID
     * 作用：
     * 1. 核心锚点：通过商品ID去数据库反查关联的【拼团活动】配置。
     * 2. 价格依据：查询该商品的原始价格。
     */
    private String goodsId;

    /** * 渠道 (Source)
     * 作用：区分流量来源（如：s01=公众号, s02=抖音, s03=自然流量）。
     * 业务含义：某些活动可能配置为“仅公众号来源可用”，规则引擎需要此字段进行过滤。
     */
    private String source;

    /** * 来源/媒介 (Channel)
     * 作用：区分端类型（如：c01=小程序, c02=App, c03=H5）。
     * 业务含义：用于匹配活动配置中的 channel 限制字段。
     */
    private String channel;

}
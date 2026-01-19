package cn.xuele.api.dto;

import lombok.Data;

/**
 * 营销拼团锁单请求参数
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 23:41
 */
@Data
public class LockMarketPayOrderRequestDTO {

    /** 用户ID，交易发起人 */
    private String userId;

    /** 拼单组队ID；若为空则表示“发起新团”，不为空则表示“参与旧团” */
    private String teamId;

    /** 活动ID，用于校验活动规则和有效期 */
    private Long activityId;

    /** 商品ID，指定要锁定的商品SKU */
    private String goodsId;

    /** 渠道标识 (如抖音、小程序)，用于统计或区分渠道库存 */
    private String source;

    /** 来源标识 (如具体的推广位ID)，用于营销归因 */
    private String channel;

    /** 外部交易单号，用于幂等性校验和支付回调关联 */
    private String outTradeNo;

    /** 回调地址 */
    private String notifyUrl;

}
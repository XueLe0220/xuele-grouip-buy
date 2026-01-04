package cn.xuele.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道商品活动配置值对象
 * <p>
 * 领域层对象，用于描述【活动】与【商品/渠道】的绑定关系。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 16:23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivityVO {

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private String goodsId;

    /** 渠道标识 (e.g., s01, s02) */
    private String source;

    /** 来源标识 (e.g., c01, c02) */
    private String channel;

}
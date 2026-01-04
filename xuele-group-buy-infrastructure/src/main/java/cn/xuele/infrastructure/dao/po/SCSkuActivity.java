package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 渠道商品活动关联配置 PO
 * <p>
 * 对应数据库表：sc_sku_activity
 * <p>
 * 核心职责：用于解耦【活动规则】与【具体商品】，实现“一个活动配置可复用于多个渠道/商品”的映射关系。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 16:09
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivity {

    /** 自增ID */
    private Long id;

    /** 渠道标识 */
    private String source;

    /** 来源标识 */
    private String channel;

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private String goodsId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
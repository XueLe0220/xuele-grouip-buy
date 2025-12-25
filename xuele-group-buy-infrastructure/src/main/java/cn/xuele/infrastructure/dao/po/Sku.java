package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息持久化对象
 * <p>
 * 对应表：sku
 *
 * @author XueLe
 * @version 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sku {

    /** 自增主键 */
    private Long id;

    /** 来源 */
    private String source;

    /** 渠道 */
    private String channel;

    /** 商品ID（业务唯一） */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原价 (使用 BigDecimal 保证金额精度) */
    private BigDecimal originalPrice;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
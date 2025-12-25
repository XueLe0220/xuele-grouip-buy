package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 持久化对象：拼团优惠配置表
 * <p>
 * 对应数据库表：group_buy_discount
 * 记录具体的优惠策略配置，如直减、满减的具体金额。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 02:36
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyDiscount {

    /** 自增ID */
    private Long id;

    /** 折扣ID (业务主键) */
    private String discountId;

    /** 折扣标题 */
    private String discountName;

    /** 折扣描述 */
    private String discountDesc;

    /** 折扣类型（0:base、1:tag） */
    private Integer discountType;

    /** 营销优惠计划（ZJ:直减、MJ:满减、N元购） */
    private String marketPlan;

    /** 营销优惠表达式 */
    private String marketExpr;

    /** 人群标签，特定优惠限定 */
    private String tagId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyDiscount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拼团折扣配置持久化层接口
 * <p>
 * 对应表：group_buy_discount
 * 职责：负责优惠力度、折扣类型、营销表达式的配置读取。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 17:14
 */
@Mapper
public interface IGroupBuyDiscountDao {

    GroupBuyDiscount queryGroupBuyActivityDiscountByDiscountId(String discountId);

}
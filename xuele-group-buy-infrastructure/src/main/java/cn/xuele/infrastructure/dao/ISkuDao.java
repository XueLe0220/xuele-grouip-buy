package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品信息持久化层接口
 * <p>
 * 对应表：sku
 * 职责：负责商品基础信息（特别是原价 original_price）的读取。
 * 是计算优惠金额的基准数据源。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 17:14
 */
@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

}
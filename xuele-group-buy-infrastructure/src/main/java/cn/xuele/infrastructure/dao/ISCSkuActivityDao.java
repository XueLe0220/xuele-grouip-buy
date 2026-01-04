package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渠道商品活动关联配置 DAO
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 16:11
 */
@Mapper
public interface ISCSkuActivityDao {

    /**
     * 根据 商品ID 查询关联的 活动ID
     */
    SCSkuActivity querySCSkuActivityVOBySCGoodsId(String goodsId);
}

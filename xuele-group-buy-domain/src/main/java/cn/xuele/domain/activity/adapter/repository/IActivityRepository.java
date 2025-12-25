package cn.xuele.domain.activity.adapter.repository;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 23:15
 */
public interface IActivityRepository {

    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(String source, String channel);

    SkuVO querySkuByGoodsId(String goodsId);
}

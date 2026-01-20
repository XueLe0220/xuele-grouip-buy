package cn.xuele.domain.activity.adapter.repository;

import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SCSkuActivityVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;

/**
 * 活动领域仓储接口
 * <p>
 * 负责拼团活动相关数据的查询。
 * 将基础设施层的数据访问能力暴露给 Domain 层使用。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 23:15
 */
public interface IActivityRepository {

    /**
     * 查询拼团活动的优惠配置
     */
    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId);

    /**
     * 根据商品ID查询商品SKU信息
     */
    SkuVO querySkuByGoodsId(String goodsId);

    /**
     * 根据商品ID查询商品与活动的关联信息
     */
    SCSkuActivityVO querySCSkuActivityBySCGoodsId(String goodsId, String source, String channel);

    boolean downgradeSwitch();

    boolean cutRange(String userId);

    boolean isUserInTag(String userId);
}
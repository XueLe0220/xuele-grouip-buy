package cn.xuele.infrastructure.adapter.repository;


import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.activity.model.valobj.DiscountTypeEnum;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SCSkuActivityVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import cn.xuele.infrastructure.dao.ICrowdTagsDetailDao;
import cn.xuele.infrastructure.dao.IGroupBuyActivityDao;
import cn.xuele.infrastructure.dao.IGroupBuyDiscountDao;
import cn.xuele.infrastructure.dao.ISCSkuActivityDao;
import cn.xuele.infrastructure.dao.ISkuDao;
import cn.xuele.infrastructure.dao.po.GroupBuyActivity;
import cn.xuele.infrastructure.dao.po.GroupBuyDiscount;
import cn.xuele.infrastructure.dao.po.SCSkuActivity;
import cn.xuele.infrastructure.dao.po.Sku;
import cn.xuele.infrastructure.dcc.DCCService;
import cn.xuele.types.common.RedisBitMapUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

/**
 * 拼团活动仓储实现类
 * <p>
 * 核心职责：
 * 1. 【屏蔽底层】：将 Infrastructure 层的数据库操作封装，对 Domain 层暴露纯净的业务方法。
 * 2. 【数据转换】：负责将数据库 PO 对象 (Persistent Object) 转换为业务 VO 对象 (Value Object)。
 * 3. 【聚合查询】：将活动表、折扣表的数据聚合为一个完整的活动规则实体。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 23:22
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class ActivityRepository implements IActivityRepository {

    private final IGroupBuyActivityDao groupBuyActivityDao;
    private final IGroupBuyDiscountDao groupBuyDiscountDao;
    private final ISkuDao skuDao;
    private final ISCSkuActivityDao scSkuActivityDao;
    private final ICrowdTagsDetailDao crowdTagsDetailDao;
    private final DCCService dccService;
    private final RedissonClient redissonClient;


    @Override
    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId) {
        // 1. 构建查询参数对象
        GroupBuyActivity groupBuyActivityReq = new GroupBuyActivity();
        groupBuyActivityReq.setActivityId(activityId);

        // 2. 查询活动主体信息
        GroupBuyActivity groupBuyActivityRes = groupBuyActivityDao.queryValidGroupBuyActivity(groupBuyActivityReq);

        // [关键防御]：如果当前渠道没有配置活动，直接返回 null，防止后续空指针异常
        if (null == groupBuyActivityRes) {
            return null;
        }

        // 3. 获取折扣ID并查询关联的折扣配置
        String discountId = groupBuyActivityRes.getDiscountId();
        GroupBuyDiscount groupBuyDiscountRes =
                groupBuyDiscountDao.queryGroupBuyActivityDiscountByDiscountId(discountId);

        // [关键防御]：虽然理论上活动必须有关联折扣，但为了健壮性，若折扣不存在也需处理
        if (null == groupBuyDiscountRes) {
            log.warn("活动存在但未配置有效折扣信息. activityId:{}", groupBuyActivityRes.getActivityId());
            return null;
        }

        // 4. 组装内部聚合对象：折扣信息
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount =
                GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                        .discountName(groupBuyDiscountRes.getDiscountName())
                        .discountDesc(groupBuyDiscountRes.getDiscountDesc())
                        .discountType(DiscountTypeEnum.get(groupBuyDiscountRes.getDiscountType()))
                        .marketPlan(groupBuyDiscountRes.getMarketPlan())
                        .marketExpr(groupBuyDiscountRes.getMarketExpr())
                        .tagId(groupBuyDiscountRes.getTagId())
                        .build();

        // 5. 组装最终的聚合对象返回
        return GroupBuyActivityDiscountVO.builder()
                .activityId(groupBuyActivityRes.getActivityId())
                .activityName(groupBuyActivityRes.getActivityName())
                .groupBuyDiscount(groupBuyDiscount) // 注入折扣聚合信息
                .groupType(groupBuyActivityRes.getGroupType())
                .takeLimitCount(groupBuyActivityRes.getTakeLimitCount())
                .target(groupBuyActivityRes.getTarget())
                .validTime(groupBuyActivityRes.getValidTime())
                .status(groupBuyActivityRes.getStatus())
                .startTime(groupBuyActivityRes.getStartTime())
                .endTime(groupBuyActivityRes.getEndTime())
                .tagId(groupBuyActivityRes.getTagId())
                .tagScope(groupBuyActivityRes.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        // 1. 查询商品 PO
        Sku sku = skuDao.querySkuByGoodsId(goodsId);

        // [关键防御]：查不到商品时返回 null
        if (null == sku) {
            return null;
        }

        // 2. 转换为业务 VO
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String goodsId, String source, String channel) {
        // 1. 查询商品活动关联PO
        SCSkuActivity scSkuActivityRes = SCSkuActivity.builder()
                .goodsId(goodsId)
                .source(source)
                .channel(channel)
                .build();

        SCSkuActivity scSkuActivity = scSkuActivityDao.querySCSkuActivityVO(scSkuActivityRes);

        if (null == scSkuActivity) {
            return null;
        }

        // 2. 转换为 业务VO
        return SCSkuActivityVO.builder()
                .goodsId(goodsId)
                .activityId(scSkuActivity.getActivityId())
                .source(scSkuActivity.getSource())
                .channel(scSkuActivity.getChannel())
                .build();
    }

    @Override
    public boolean downgradeSwitch() {
        return dccService.isDowngradeSwitch();
    }

    @Override
    public boolean cutRange(String userId) {
        return dccService.isCutRange(userId);
    }

    @Override
    public boolean isUserInTag(String userId) {
        String tagId = crowdTagsDetailDao.queryTagIdByUserId(userId);
        RBitSet bitSet = redissonClient.getBitSet(RedisBitMapUtils.getTagBitMapKey(tagId));
        return bitSet.get(RedisBitMapUtils.getIndexFromUserId(userId));
    }


}
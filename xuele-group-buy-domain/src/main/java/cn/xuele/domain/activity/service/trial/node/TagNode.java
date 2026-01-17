package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.domain.tag.adapter.repository.ITagRepository;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 规则树节点：人群标签过滤节点 (TagNode)
 * <p>
 * 核心职责：
 * 1. 负责基于 Redis BitMap 进行人群资格校验。
 * 2. 识别活动配置 (可见性/参与性)，决定是否拦截当前用户。
 * 3. 将校验结果写入上下文 (DynamicContext)。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 18:41
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagNode extends AbstractGroupBuyMarketSupport {

    /**
     * 下一个执行节点
     */
    private final EndNode endNode;


    /**
     * 执行节点逻辑
     *
     * @param requestParameter 请求参数 (包含 userId, goodsId 等)
     * @param dynamicContext   动态上下文 (包含活动配置 VO 和中间计算结果)
     * @return 试算结果实体
     */
    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter,
                                         DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {

        // 1. 从上下文中获取活动配置值对象 (GroupBuyActivityDiscountVO)
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();

        String tagId = groupBuyActivityDiscountVO.getTagId();
        // 获取配置的大门状态 (true=默认开放, false=默认设防)
        boolean visible = groupBuyActivityDiscountVO.isVisible();
        boolean enable = groupBuyActivityDiscountVO.isEnable();

        // 2.【Fast Pass 策略】
        // 如果活动未配置人群标签 ID，说明该活动对所有人开放，无需查 Redis，直接放行
        if (StringUtils.isBlank(tagId)) {
            dynamicContext.setVisible(true);
            dynamicContext.setEnable(true);
            // 路由到下一个节点
            return router(requestParameter, dynamicContext);
        }

        // 3.【BitMap 校验】
        // 调用基础设施层，查询 Redis BitMap 确认用户是否在白名单中
        boolean isWithin = repository.isUserInTag(tagId, requestParameter.getUserId());

        // 4.【混合逻辑判定】
        // 核心逻辑：(活动本来就开放) || (用户拥有白名单钥匙)
        // 利用布尔短路特性：如果 visible 为 true，则不看 isWithin；如果 visible 为 false，才看 isWithin
        dynamicContext.setVisible(visible || isWithin);
        dynamicContext.setEnable(enable || isWithin);

        // 5. 继续路由
        return router(requestParameter, dynamicContext);
    }

    /**
     * 获取下一个责任链节点
     */
    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return endNode;
    }
}
package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 拼团交易试算 - 异常校验节点
 * <p>
 * 核心职责：
 * 1. 作为规则树执行流程中的兜底环节。
 * 2. 检查上下文中是否已成功加载【活动优惠配置(DiscountVO)】和【商品信息(SkuVO)】。
 * 3. 如果关键上下文缺失，直接抛出业务异常，阻断后续流程，防止空指针或无效计算。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 16:45
 */
@Slf4j
@Service
public class ErrorNode extends AbstractGroupBuyMarketSupport {

    /**
     * 执行校验逻辑
     *
     * @param requestParameter 请求入参（包含用户ID、商品ID、渠道来源等）
     * @param dynamicContext   动态上下文（包含预加载的活动配置、SKU信息等）
     * @return 试算结果实体（通常此节点抛出异常，若通过则返回空对象继续后续流程）
     * @throws Exception 当检测到无营销配置时，抛出 E0002 异常
     */
    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-NoMarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // 校验：检查上下文中是否包含必要的营销配置和商品信息
        if (null == dynamicContext.getGroupBuyActivityDiscountVO() || null == dynamicContext.getSkuVO()) {
            log.info("商品无拼团营销配置 {}", requestParameter.getGoodsId());
            // 抛出明确的业务异常，告知上层“该商品不参与活动”
            throw new AppException(ResponseCode.E0002.getCode(), ResponseCode.E0002.getInfo());
        }

        // 校验通过，返回空的构建对象（后续节点会填充具体数据）
        return TrialBalanceEntity.builder().build();
    }

    /**
     * 获取策略处理器路由
     *
     * @return 返回默认的策略处理器，继续链路执行
     */
    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

}
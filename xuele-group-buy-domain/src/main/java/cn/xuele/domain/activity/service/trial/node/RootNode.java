package cn.xuele.domain.activity.service.trial.node;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import cn.xuele.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import cn.xuele.types.enums.ResponseCode;
import cn.xuele.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 规则树根节点 (Root Node)
 *
 * @author XueLe
 * @version 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RootNode extends AbstractGroupBuyMarketSupport {

    private final SwitchNode switchNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter,
                                      DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 1. 日志记录
        log.info("拼团商品查询试算业务-RootNode Start. userId:{} goodsId:{}",
                requestParameter.getUserId(), requestParameter.getGoodsId());

        // 2. 参数校验
        if (StringUtils.isBlank(requestParameter.getUserId()) ||
                StringUtils.isBlank(requestParameter.getGoodsId()) ||
                StringUtils.isBlank(requestParameter.getChannel()) ||
                StringUtils.isBlank(requestParameter.getSource())) {

            // 错误日志建议用 Error 或 Warn，并打印完整参数以便排查
            log.warn("拼团试算参数校验失败. params:{}", JSON.toJSONString(requestParameter));
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 3. 路由转发
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return switchNode;
    }
}
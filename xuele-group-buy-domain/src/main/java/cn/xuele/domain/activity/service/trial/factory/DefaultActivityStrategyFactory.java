package cn.xuele.domain.activity.service.trial.factory;

import cn.xuele.domain.activity.model.entity.MarketProductEntity;
import cn.xuele.domain.activity.model.entity.TrialBalanceEntity;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import cn.xuele.domain.activity.service.trial.node.RootNode;
import cn.xuele.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 活动策略工厂 (默认实现)
 * <p>
 * 职责描述：
 * 1. 【策略容器】：管理并持有拼团试算规则树的根节点 {@link RootNode}。
 * 2. 【执行入口】：向领域服务层提供统一的策略执行入口，屏蔽内部规则树的复杂结构。
 * 3. 【上下文定义】：定义策略执行过程中所需的动态上下文 {@link DynamicContext}。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 09:25
 */
@Service
@RequiredArgsConstructor
public class DefaultActivityStrategyFactory {

    /** 持有规则树的根节点 */
    private final RootNode rootNode;

    /**
     * 获取策略执行器
     * <p>
     * 外部服务调用此方法获取规则树的入口，进而执行 .apply() 方法
     *
     * @return 策略处理接口 (实际指向 RootNode)
     */
    public StrategyHandler<MarketProductEntity, DynamicContext, TrialBalanceEntity> strategyHandler() {
        return rootNode;
    }

    /**
     * 动态上下文
     * <p>
     * 作用：在策略链流转过程中，承载动态获取的配置数据（如：从数据库查出的活动规则、折扣参数）。
     * 区别：
     * - MarketProductEntity (入参)：前端传来的，不可变。
     * - DynamicContext (上下文)：后端查出来的，随着节点流转可能会被填充数据。
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class DynamicContext {
        private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
        private SkuVO skuVO;
        private BigDecimal deductionPrice;
        private BigDecimal payPrice;
    }
}
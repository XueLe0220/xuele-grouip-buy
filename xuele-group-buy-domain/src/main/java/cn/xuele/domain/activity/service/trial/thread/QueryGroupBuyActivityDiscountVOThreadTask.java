package cn.xuele.domain.activity.service.trial.thread;

import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.xuele.domain.activity.model.valobj.SCSkuActivityVO;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

/**
 * 拼团活动配置查询任务 (异步线程任务)
 * <p>
 * 职责描述：
 * 1. 封装"查询活动配置"的原子操作。
 * 2. 实现 {@link Callable} 接口，允许被提交到线程池执行并返回结果。
 * 3. 用于 {@link cn.xuele.domain.activity.service.trial.node.MarketNode} 节点的异步编排场景。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 23:14
 */
@RequiredArgsConstructor
public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    /** 查询参数：商品Id */
    private final String goodsId;

    /** 仓储接口  */
    private final IActivityRepository repository;

    /**
     * 执行查询逻辑
     *
     * @return 拼团活动聚合对象 (Activity + Discount)
     * @throws Exception 查询过程中的异常
     */
    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(goodsId);
        if (null == scSkuActivityVO) {
            return null;
        }
        return repository.queryGroupBuyActivityDiscountVO(scSkuActivityVO.getActivityId());
    }
}
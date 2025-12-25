package cn.xuele.domain.activity.service.trial.thread;

import cn.xuele.domain.activity.adapter.repository.IActivityRepository;
import cn.xuele.domain.activity.model.valobj.SkuVO;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;

/**
 * 商品信息查询任务 (异步线程任务)
 * <p>
 * 职责描述：
 * 1. 封装"查询商品基础信息"的原子操作。
 * 2. 重点在于获取商品的【原价】，作为后续优惠计算的基准。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 23:45
 */
@RequiredArgsConstructor
public class QuerySkuVOThreadTask implements Callable<SkuVO> {


    /** 商品ID */
    private final String goodsId;

    /** 仓储接口 */
    private final IActivityRepository repository;


    /**
     * 执行查询逻辑
     *
     * @return 商品值对象 (含原价)
     * @throws Exception 查询过程中的异常
     */
    @Override
    public SkuVO call() throws Exception {
        return repository.querySkuByGoodsId(goodsId);
    }
}
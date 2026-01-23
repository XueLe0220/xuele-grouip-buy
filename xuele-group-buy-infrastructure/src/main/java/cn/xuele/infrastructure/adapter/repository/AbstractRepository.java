package cn.xuele.infrastructure.adapter.repository;

import cn.xuele.infrastructure.dcc.DCCService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 仓储抽象基类
 * <p>
 * 提供标准化的“缓存+数据库”双层查询模版方法。
 * 封装了 缓存命中、回写、DCC动态降级 的通用逻辑，减少重复代码。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/23 13:30
 */
@RequiredArgsConstructor
public abstract class AbstractRepository {

    private final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);
    protected final RedissonClient redissonClient;
    protected final DCCService dccService;


    /**
     * 通用缓存查询方法（无过期时间）
     * <p>
     * 逻辑：优先查缓存 -> 未命中查库 -> 结果回写缓存（永久有效）
     * 场景：适用于字典表等极少变更的数据
     */
    protected <T> T getFromCacheOrDB(String cacheKey, Supplier<T> dbFallback) {
        // 1. 检查 DCC 降级开关
        if (dccService.isCacheOpenSwitch()) {
            RBucket<T> bucket = redissonClient.<T>getBucket(cacheKey);
            T cacheResult = bucket.get();

            // 2. 缓存命中直接返回
            if (null != cacheResult) {
                return cacheResult;
            }

            // 3. 缓存未命中，执行数据库查询
            T dbResult = dbFallback.get();
            if (null == dbResult) {
                return null;
            }

            // 4. 数据库结果回写缓存
            bucket.set(dbResult);
            return dbResult;
        } else {
            // 缓存开关关闭，降级为直连数据库
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }

    /**
     * 通用缓存查询方法（带过期时间）
     * <p>
     * 场景：适用于大多数业务数据，通过 TTL 保证最终一致性
     *
     * @param expired 缓存过期时间（单位：秒）
     */
    protected <T> T getFromCacheOrDB(String cacheKey, Supplier<T> dbFallback, long expired) {
        if (dccService.isCacheOpenSwitch()) {
            RBucket<T> bucket = redissonClient.<T>getBucket(cacheKey);
            T cacheResult = bucket.get();

            if (null != cacheResult) {
                return cacheResult;
            }

            T dbResult = dbFallback.get();
            if (null == dbResult) {
                return null;
            }

            // 回写缓存并设置过期时间
            bucket.set(dbResult, Duration.ofSeconds(expired));
            return dbResult;
        } else {
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }
}
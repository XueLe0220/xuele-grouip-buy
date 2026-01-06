package cn.xuele.infrastructure.dcc;

import cn.xuele.types.annotation.DCCValue;
import org.springframework.stereotype.Service;

/**
 * 动态配置中心服务 (DCC Service)
 * <p>
 * 作用：
 * 1. 作为配置的“容器”，持有当前内存中的配置值。
 * 2. 提供配置读取的业务方法（如判断是否降级、判断是否切量）。
 * <p>
 * 实现原理：
 * 字段被 @DCCValue 标记后，DCC 框架会在启动时和运行时自动注入 Redis 中的最新值。
 *
 * @author XueLe
 * @version 1.0.1
 * @since 2026/01/04 23:14
 */
@Service
public class DCCService {

    /**
     * 降级开关
     * <p>
     * 格式约定："RedisKey:默认值"
     * key="downgradeSwitch", default="0" (0=不降级, 1=降级)
     * <p>
     * 注意：必须使用 volatile 保证多线程可见性
     */
    @DCCValue("downgradeSwitch:0")
    private volatile String downgradeSwitch;

    /**
     * 切量范围
     * <p>
     * 格式约定："RedisKey:默认值"
     * key="cutRange", default="100"
     */
    @DCCValue("cutRange:100")
    private volatile String cutRange;

    /**
     * 判断是否触发降级
     *
     * @return true=触发降级(拦截), false=正常通行
     */
    public boolean isDowngradeSwitch() {
        // 对应 Redis 中的 value 为 "1" 时开启降级
        return "1".equals(downgradeSwitch);
    }

    /**
     * 判断用户是否在切量范围内 (灰度控制)
     *
     * @param userId 用户ID
     * @return true=在范围内(放行), false=不在范围内(拦截/走旧逻辑)
     */
    public boolean isCutRange(String userId) {
        // 1. 防御性编程：防止配置未加载或 userId 为空
        if (userId == null || cutRange == null) {
            return false;
        }

        try {
            // 2. 计算用户哈希 (使用绝对值防止负数)
            // 解释：hashCode 是离散的，取模 100 可以把用户均匀分成 0-99 个桶
            int hashCode = Math.abs(userId.hashCode());
            int lastTwoDigits = hashCode % 100;

            // 3. 解析配置阈值 (建议：后续优化可将 parseInt 移到更新配置时执行，避免每次请求都解)
            int limit = Integer.parseInt(cutRange);

            // 4. 判断逻辑：
            // 假设 limit = 10，则 0-10 (共11个桶) 通过。
            return lastTwoDigits <= limit;

        } catch (NumberFormatException e) {
            // 如果 Redis 配置了非法字符，默认返回 false 或 true (根据业务容错策略)
            // 这里选择 false，安全起见不让通过
            return false;
        }
    }
}
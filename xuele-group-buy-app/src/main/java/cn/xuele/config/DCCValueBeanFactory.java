package cn.xuele.config;

import cn.xuele.types.annotation.DCCValue;
import cn.xuele.types.common.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * DCC 动态配置扫描与监听处理器
 * <p>
 * 核心职能：
 * 1. 启动阶段 (BeanPostProcessor)：扫描 @DCCValue 注解，初始化 Redis 配置，并注入初始值。
 * 2. 运行阶段 (Redisson Listener)：监听 Redis Pub/Sub 消息，动态更新内存中的配置值。
 *
 * @author XueLe
 * @version 1.0.1
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DCCValueBeanFactory implements BeanPostProcessor {

    // Redis 配置 Key 的统一前缀，防止与业务 Key 冲突
    private static final String BASE_CONFIG_PATH = "group_buy_market_dcc_";

    private final RedissonClient redissonClient;

    /**
     * DCC 动态配置对象注册表 (本地索引)
     * <p>
     * Key: Redis 中的完整 Key (例如 group_buy_market_dcc_downgradeSwitch)
     * Value: 这是一个强引用的目标对象 (Target Object)。
     * <p>
     * 作用：当 Listener 收到 Redis 变更通知时，通过此 Map 快速找到内存中需要修改的对象实例。
     */
    @Getter
    private final Map<String, Object> dccObjGroup = new HashMap<>();

    /**
     * 注册 Redis 监听器 (监听配置变更)
     * <p>
     * 这里的逻辑是 DCC 的“耳朵”。
     * 当控制台发布 Pub/Sub 消息时，此方法内的回调函数会被触发。
     *
     * @param redissonClient Redisson 客户端
     * @return RTopic 返回 Topic 对象交给 Spring 管理，方便扩展（如手动发送消息）
     */
    @Bean("dccTopic")
    public RTopic dccRedisTopicListener(RedissonClient redissonClient) {
        // 1. 订阅指定 Topic
        RTopic topic = redissonClient.getTopic("group_buy_market_dcc");

        // 2. 添加监听逻辑
        topic.addListener(String.class, (charSequence, s) -> {
            try {
                // 消息格式约定：Key后缀,新值 (例如: "downgradeSwitch,1")
                String[] split = s.split(Constants.SPLIT);

                String attribute = split[0];
                String key = BASE_CONFIG_PATH + attribute; // 拼接完整 Key
                String value = split[1];

                // 3. 【持久化】将新值写入 Redis (防止重启后配置回滚)
                // Pub/Sub 只是通知通道，不负责存储，所以必须手动 Set
                RBucket<String> bucket = redissonClient.getBucket(key);
                if (!bucket.isExists()) {
                    return; // 如果 Key 不存在，说明不是合法配置，忽略
                }
                bucket.set(value);

                // 4. 【寻址】从本地注册表中找到需要更新的对象
                Object objBean = dccObjGroup.get(key);
                if (null == objBean) {
                    return;
                }

                // 5. 【AOP解包】确保拿到的是“真身” (Target)，而不是“替身” (Proxy)
                // 如果是代理对象，必须解包，否则反射修改可能不生效或报错
                Class<?> objBeanClass = objBean.getClass();
                if (AopUtils.isAopProxy(objBean)) {
                    // 修正图纸 (Class) 为目标类
                    objBeanClass = AopUtils.getTargetClass(objBean);
                    // 修正引用 (Object) 为目标实例
                    objBean = AopProxyUtils.getSingletonTarget(objBean);
                }

                // 再次防御性判空 (防止解包失败)
                if (objBean == null) return;

                // 6. 【反射注入】暴力修改内存中的私有变量
                // getDeclaredField 可以获取 private/protected 字段
                Field field = objBeanClass.getDeclaredField(attribute);
                field.setAccessible(true);
                field.set(objBean, value); // 修改内存生效
                field.setAccessible(false);

                log.info("DCC 动态配置更新完成 Key:{} -> Val:{}", key, value);

            } catch (Exception e) {
                log.error("DCC 监听异常", e);
                // 监听器异常不建议抛出 RuntimeException 阻断线程，打印日志即可
            }
        });
        return topic;
    }

    /**
     * Spring Bean 初始化后置处理器
     * <p>
     * 这里的逻辑是 DCC 的“眼睛”。
     * 在 Spring 启动创建 Bean 的过程中，扫描字段上的 @DCCValue 注解。
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // 1. AOP 预处理：不管是普通 Bean 还是代理 Bean，都要拿到真实的目标类
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;

        if (AopUtils.isAopProxy(bean)) {
            targetBeanClass = AopUtils.getTargetClass(bean);
            targetBeanObject = AopProxyUtils.getSingletonTarget(bean);
        }

        // 如果无法获取目标对象，直接跳过
        if (targetBeanObject == null) return bean;

        // 2. 遍历所有字段
        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            // 只处理带 @DCCValue 的字段
            if (!field.isAnnotationPresent(DCCValue.class)) {
                continue;
            }

            // 3. 解析注解信息
            DCCValue dccValue = field.getAnnotation(DCCValue.class);
            String value = dccValue.value();

            if (StringUtils.isBlank(value)) {
                throw new RuntimeException("字段 " + field.getName() + " 的 @DCCValue 配置为空，请按照 'key:default' 格式配置");
            }

            // 拆解 Key 和 默认值
            String[] split = value.split(":");
            String key = BASE_CONFIG_PATH.concat(split[0]);
            // 容错处理：防止数组越界
            String defaultValue = split.length == 2 ? split[1] : null;

            // 强校验：默认值必须存在
            if (StringUtils.isBlank(defaultValue)) {
                throw new RuntimeException("DCC配置错误：Key " + key + " 必须设置默认值！");
            }

            String setValue;
            try {
                // 4. Redis 初始化检查
                // 如果 Redis 里没有这个 Key，说明是新上线的配置，自动用默认值初始化 Redis
                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean exists = bucket.isExists();

                if (!exists) {
                    bucket.set(defaultValue);
                    setValue = defaultValue;
                } else {
                    // 如果 Redis 里有了，以 Redis 为准 (持久化值)
                    setValue = bucket.get();
                }

                // 5. 初始值注入
                field.setAccessible(true);
                field.set(targetBeanObject, setValue);
                field.setAccessible(false);

                // 6. 【注册】将该对象登记到 Map 中，供监听器后续使用
                dccObjGroup.put(key, targetBeanObject);

            } catch (Exception e) {
                log.error("DCC 启动注入失败 beanName:{}", beanName, e);
                throw new RuntimeException(e);
            }
        }
        return bean;
    }
}
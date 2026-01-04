package cn.xuele.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置类
 * <p>
 * 负责初始化 RedissonClient 实例，并从 application.yml 中加载配置项。
 * 采用 JsonJacksonCodec 作为默认编解码器，支持对象直接存取。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/03 18:48
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /**
     * 注入 RedissonClient 实例
     *
     * @param applicationContext Spring应用上下文
     * @param properties         Redis配置属性
     * @return RedissonClient 客户端实例
     */
    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext,
                                         RedisClientConfigProperties properties) {
        Config config = new Config();

        // 设置编解码器：使用 Jackson 将对象序列化为 JSON
        // 优点：可读性好，支持复杂对象；缺点：存储空间稍大（含类名）
        config.setCodec(JsonJacksonCodec.INSTANCE);

        // 使用单机模式 (Single Server)
        // 注意：Redisson 强制要求地址以 redis:// 开头
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                // .setPassword(properties.getPassword()) // 如果 Redis 有密码，请取消注释
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        return Redisson.create(config);
    }
}
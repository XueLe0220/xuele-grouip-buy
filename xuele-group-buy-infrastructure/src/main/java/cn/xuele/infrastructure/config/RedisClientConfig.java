package cn.xuele.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext,
                                         RedisClientConfigProperties properties) {
        Config config = new Config();

        // ================== 修改开始 ==================
        // 1. 创建自定义的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 核心解药：手动注册 Java 8 时间模块
        // (前提：你的 infrastructure 模块必须引入了 jackson-datatype-jsr310 依赖)
        objectMapper.registerModule(new JavaTimeModule());

        // 3. (可选优化) 设置为不将日期写为时间戳，这样在 Redis 里的数据是 "2026-01-23T..." 的字符串，可读性更强
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 4. 使用自定义的 mapper 创建编解码器，而不是用默认的 INSTANCE
        config.setCodec(new JsonJacksonCodec(objectMapper));
        // ================== 修改结束 ==================

        // 使用单机模式
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
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
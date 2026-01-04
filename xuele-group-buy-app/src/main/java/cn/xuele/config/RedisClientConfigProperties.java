package cn.xuele.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 连接配置属性映射类
 * <p>
 * 对应 application.yml 中 redis.sdk.config 下的配置项
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/03 18:48
 */
@Data
@ConfigurationProperties(prefix = "redis.sdk.config", ignoreInvalidFields = true)
public class RedisClientConfigProperties {

    /** Redis 服务器地址 (IP) */
    private String host;

    /** Redis 端口 */
    private int port;

    /** 认证密码 (如无密码可留空) */
    private String password;

    /** * 连接池大小
     * <p>默认为 64。Redisson 基于 Netty 异步非阻塞模型，
     * 不需要像 Jedis 那样配置几百个连接，少量连接即可支持高并发。</p>
     */
    private int poolSize = 64;

    /** * 最小空闲连接数
     * <p>默认为 10。保留少量空闲连接以应对突发请求，避免频繁创建连接。</p>
     */
    private int minIdleSize = 10;

    /** * 空闲连接超时时间 (毫秒)
     * <p>默认为 10000。连接空闲超过此时间将被回收，防止资源浪费。</p>
     */
    private int idleTimeout = 10000;

    /** * 连接建立超时时间 (毫秒)
     * <p>默认为 10000。网络拥堵时，超过此时间判定为连接失败。</p>
     */
    private int connectTimeout = 10000;

    /** * 命令重试次数
     * <p>默认为 3。命令执行失败后的重试次数。</p>
     */
    private int retryAttempts = 3;

    /** * 命令重试间隔 (毫秒)
     * <p>默认为 1000。两次重试之间的等待时间。</p>
     */
    private int retryInterval = 1000;

    /** * 定期 Ping 检查间隔 (毫秒)
     * <p>默认为 0 (禁用)。用于检测连接是否存活，防止网络静默断开。</p>
     */
    private int pingInterval = 0;

    /** * 是否启用 TCP KeepAlive
     * <p>默认为 true。保持长连接活跃。</p>
     */
    private boolean keepAlive = true;
}
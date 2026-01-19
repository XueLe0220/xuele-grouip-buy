package cn.xuele.infrastructure.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP 客户端配置类
 * * @author XueLe
 */
@Configuration
public class HttpConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // 默认用 1.1，兼容性好。也可以设为 HTTP_2
                .connectTimeout(Duration.ofSeconds(5))  // 【关键配置】连接超时：5秒。连不上就别连了，防止卡死线程
                .followRedirects(HttpClient.Redirect.NORMAL) // 允许自动重定向
                .build();
    }
}
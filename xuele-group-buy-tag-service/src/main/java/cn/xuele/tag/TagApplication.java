package cn.xuele.tag;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 人群标签服务启动类
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/05/31 10:35
 */
@SpringBootApplication
@EnableDubbo
public class TagApplication {
    public static void main(String[] args) {
        SpringApplication.run(TagApplication.class, args);
    }
}
package cn.xuele.infrastructure.gateway;

import cn.xuele.types.enums.NotifyTaskHTTPEnumVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 拼团回调通知网关服务 (Java 21 原生 HttpClient 版)
 *
 * @author XueLe
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyNotifyService {

    private final HttpClient httpClient;

    /**
     * 发送回调通知
     *
     * @param apiUrl               回调地址
     * @param notifyRequestDTOJSON 请求参数 (JSON字符串)
     * @return 第三方响应的 Body 字符串
     * @throws Exception 网络异常抛出给上层处理
     */
    public String groupBuyNotify(String apiUrl, String notifyRequestDTOJSON) throws Exception {
        try {
            // 1. 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json") // 设置 Header
                    .POST(HttpRequest.BodyPublishers.ofString(notifyRequestDTOJSON)) // 设置 Body (OkHttp 的 RequestBody)
                    .timeout(Duration.ofSeconds(3)) // 【关键配置】读取超时：3秒。对方3秒不回话，直接断开，视为失败
                    .build();

            // 2. 发送请求 (相当于 OkHttp 的 execute)
            // BodyHandlers.ofString() 意思是：把响应体直接转成 String 给我
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 3. 检查状态码 (这是 OkHttp 教学代码里漏掉的！)
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                // 请求成功，返回对方给的响应体 (比如 "success")
                return response.body();
            } else {
                log.warn("拼团回调 HTTP 请求失败, code: {}, url: {}", statusCode, apiUrl);
                // 对方服务器报错了 (404, 500 等)，这不算我们的程序异常，但业务上是失败的
                // 这里可以直接返回 null 或者特定的错误码，让上层去重试
                return NotifyTaskHTTPEnumVO.NULL.getCode();
            }

        } catch (Exception e) {
            log.error("拼团回调 HTTP 接口服务异常, url: {}", apiUrl, e);
            // 这里直接抛出异常，让上层 Port 的 try-catch 捕获，
            // 从而触发你写的 Thread.currentThread().interrupt() 和返回 NULL 逻辑
            throw e;
        }
    }
}
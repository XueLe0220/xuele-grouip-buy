package cn.xuele.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HTTP 回调任务执行结果枚举
 * <p>
 * 用于定义 Gateway 层与 External 系统交互后的标准反馈信号
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:26
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum NotifyTaskHTTPEnumVO {

    /** 远程调用成功 (HTTP 200) */
    SUCCESS("success", "成功"),

    /** 远程调用失败 (HTTP 4xx/5xx 或 网络异常) */
    ERROR("error", "失败"),

    /**
     * 空执行 / 跳过
     * <p>
     * 场景：分布式锁抢占失败、参数校验未通过。
     * 含义：表示本次并未发起真实的 HTTP 请求，上层业务无需更新数据库状态。
     */
    NULL(null, "空执行"),
    ;

    private String code;
    private String info;
}
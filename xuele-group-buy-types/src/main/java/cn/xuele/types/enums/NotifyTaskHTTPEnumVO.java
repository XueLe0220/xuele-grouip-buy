package cn.xuele.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:26
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum NotifyTaskHTTPEnumVO {
    SUCCESS("success", "成功"),
    ERROR("error", "失败"),
    NULL(null, "空执行"),
    ;

    private String code;
    private String info;
}

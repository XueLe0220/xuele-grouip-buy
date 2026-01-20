package cn.xuele.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 回调方式枚举
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 12:41
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyTypeEnumVO {

    HTTP("HTTP", "HTTP 回调"),
    MQ("MQ", "MQ 消息通知"),
    ;

    private String code;
    private String info;

}
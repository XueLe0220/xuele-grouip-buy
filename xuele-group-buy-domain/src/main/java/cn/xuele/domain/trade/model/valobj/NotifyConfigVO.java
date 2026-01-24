package cn.xuele.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调配置值对象
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/20 12:42
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyConfigVO {

    /**
     * 回调方式；MQ、HTTP
     */
    private NotifyTypeEnumVO notifyType;
    /**
     * MQ 消息路由键 (RoutingKey)
     */
    private String notifyMQ;
    /**
     * 回调地址
     */
    private String notifyUrl;

}

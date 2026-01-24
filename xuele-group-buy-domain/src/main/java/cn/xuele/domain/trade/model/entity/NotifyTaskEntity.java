package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 交易结算通知任务聚合根/实体
 * <p>
 * 对应表：notify_task
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:19
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotifyTaskEntity implements Serializable {

    private static final int MAX_RETRY_LIMIT = 5;

    /**
     * 拼单组队ID
     */
    private String teamId;

    /**
     * 回调类型
     */
    private String notifyType;

    /**
     * MQ 消息路由键 (RoutingKey)
     */
    private String notifyMQ;

    /**
     * 回调接口
     */
    private String notifyUrl;

    /**
     * 回调次数
     */
    private Integer notifyCount;

    /**
     * 参数对象
     */
    private String parameterJson;


    public boolean hasRetryChance(){
        return this.notifyCount != null && this.notifyCount < MAX_RETRY_LIMIT;
    }

    public void increaseRetryCount() {
        this.notifyCount++;
    }
}
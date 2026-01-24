package cn.xuele.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象：拼团回调通知任务
 * <p>
 * 对应数据库表：notify_task
 * 作用：记录每一次拼团成功的异步通知任务状态
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/18 16:24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTask {

    /** 自增主键 ID */
    private Long id;

    /** 营销活动 ID */
    private Long activityId;

    /** 拼单组队 ID (业务唯一索引) */
    private String teamId;

    /** 回调类型 */
    private String notifyType;

    /** MQ 消息路由键 (RoutingKey) */
    private String notifyMQ;

    /** 回调通知地址 (HTTP接口) */
    private String notifyUrl;

    /** 已重试次数 */
    private Integer notifyCount;

    /** 通知状态 */
    private Integer notifyStatus;

    /** 请求参数 JSON 字符串 */
    private String parameterJson;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
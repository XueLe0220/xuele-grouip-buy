package cn.xuele.domain.trade.adapter.port;

import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;

/**
 * 交易结算域-防腐层端口接口 (ACL Port)
 * <p>
 * 架构定位：Domain Layer (领域层)
 * 职责：定义交易领域与外部系统（或基础设施）交互的标准契约，将业务逻辑与具体的 HTTP/RPC 实现解耦。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:15
 */
public interface ITradePort {

    /**
     * 执行拼团成功的回调通知
     * <p>
     * 场景：当拼团撞线成功后，通过此端口向外部（第三方应用）发送 HTTP 通知。
     * 实现约定：实现类需包含“分布式锁抢占”机制，确保高并发下同一任务仅被执行一次。
     *
     * @param notifyTask 通知任务领域实体（包含回调URL、TeamID、请求参数）
     * @return 执行结果状态码 (对应 NotifyTaskHTTPEnumVO，例如 "success"、"error" 或 "null")
     * @throws Exception 可能会抛出网络超时或连接异常，建议由上层统一捕获处理
     */
    String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception;
}
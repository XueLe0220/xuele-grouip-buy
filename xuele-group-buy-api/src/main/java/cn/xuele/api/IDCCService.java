package cn.xuele.api;

import cn.xuele.api.response.Response;

/**
 * DCC 动态配置中心服务接口 (DCC Service Interface)
 * <p>
 * 作用：
 * 提供给外部（如管理后台、定时任务）调用的统一入口。
 * 调用此接口可以修改配置，并触发 Redis 推送，使所有监听的客户端实时生效。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/05 15:18
 */
public interface IDCCService {

    /**
     * 更新配置
     * <p>
     * 发布 Redis Pub/Sub 消息 (触发各节点的本地缓存更新)
     *
     * @param key   配置键 (例如: downgradeSwitch)
     * @param value 配置值 (例如: 1)
     * @return Response<Boolean> true=发送成功
     */
    Response<Boolean> updateConfig(String key, String value);
}
package cn.xuele.trigger.http;

import cn.xuele.api.IDCCService;
import cn.xuele.api.response.Response;
import cn.xuele.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.springframework.web.bind.annotation.*;

/**
 * DCC (Dynamic Configuration Center) 动态配置控制器
 * <p>
 * 职责：接收外部管理平台的配置变更请求，并通过 Redis Pub/Sub 广播给所有服务节点。
 * 作用：实现应用配置的“热更新”和“降级开关”的实时生效，无需重启服务。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/05 15:22
 */
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/v1/gbm/dcc/")
public class DCCController implements IDCCService {

    @Resource
    private RTopic dccTopic;

    /**
     * 更新动态配置
     * <p>
     * 原理：向 Redis Topic 发布消息，各节点监听该 Topic 并更新本地 JVM 内存缓存。
     *
     * @param key   配置键 (如: downgradeSwitch)
     * @param value 配置值 (如: 1 或 0)
     * @return 操作结果
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        try {
            log.info("DCC 动态配置变更通知 key:{} value:{}", key, value);
            // 发布消息，格式约定为 "key,value"
            dccTopic.publish(key + "," + value);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置变更失败 key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
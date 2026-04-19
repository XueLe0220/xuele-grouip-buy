package cn.xuele.domain.trade.service.task;

import cn.xuele.domain.trade.adapter.port.ITradePort;
import cn.xuele.domain.trade.adapter.repository.ITradeRepository;
import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;
import cn.xuele.domain.trade.service.ITradeTaskService;
import cn.xuele.types.enums.NotifyTaskHTTPEnumVO;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易任务服务实现类（MQ、HTTP）
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/24 12:19
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeTaskService implements ITradeTaskService {


    private final ITradeRepository repository;
    private final ITradePort port;

    @Override
    public Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception {
        log.info("拼团交易-执行结算通知回调，指定 teamId:{} notifyTaskEntity:{}", notifyTaskEntity.getTeamId(),
                JSON.toJSONString(notifyTaskEntity));
        return execNotifyJob(Collections.singletonList(notifyTaskEntity));
    }

    @Override
    public Map<String, Integer> execNotifyJob() {
        log.info("拼团交易-执行结算通知任务");

        // 查询未执行任务
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList();

        return execNotifyJob(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execNotifyJob(String teamId) {
        // 1. 日志：明确入参
        log.info("执行指定拼团结算通知任务, teamId: {}", teamId);

        // 2. 查库：这里后续要在 Repository 实现中注意，必须查不到返回空List，不能返回null，防止下面空指针
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList(teamId);

        // 3. 执行
        return execNotifyJob(notifyTaskEntityList);
    }

    private Map<String, Integer> execNotifyJob(List<NotifyTaskEntity> notifyTaskEntityList) {
        Map<String, Integer> resultMap = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int retryCount = 0;

        if (notifyTaskEntityList == null || notifyTaskEntityList.isEmpty()) {
            return resultMap;
        }

        for (NotifyTaskEntity notifyTask : notifyTaskEntityList) {
            try {
                // 执行 HTTP 通知
                String response = port.groupBuyNotify(notifyTask);

                // 1. 成功
                if (NotifyTaskHTTPEnumVO.SUCCESS.getCode().equals(response)) {
                    int updateCount = repository.updateNotifyTaskStatusSuccess(notifyTask);
                    if (updateCount > 0) successCount++;
                }
                // 2. 失败 (业务逻辑层面的失败，如 404/500)
                else {
                    // 内存先自增，保证逻辑闭环
                    notifyTask.increaseRetryCount();

                    if (notifyTask.hasRetryChance()) {
                        // 此时 task 里的 count 已经是+1后的值了
                        int updateCount = repository.updateNotifyTaskStatusRetry(notifyTask);
                        if (updateCount > 0) retryCount++;
                    } else {
                        int updateCount = repository.updateNotifyTaskStatusError(notifyTask);
                        if (updateCount > 0) failCount++;
                    }
                }
            } catch (Exception e) {
                // 3. 异常 (代码执行层面的失败，如超时、NPE)
                log.error("结算通知任务执行异常, teamId: {}", notifyTask.getTeamId(), e);

                // 异常情况下，也要更新数据库，否则会死循环或状态丢失
                notifyTask.increaseRetryCount();
                try {
                    // 同样判断是否还能重试
                    if (notifyTask.hasRetryChance()) {
                        repository.updateNotifyTaskStatusRetry(notifyTask);
                        retryCount++;
                    } else {
                        repository.updateNotifyTaskStatusError(notifyTask);
                        failCount++;
                    }
                } catch (Exception ex) {
                    log.error("数据库更新异常, teamId: {}", notifyTask.getTeamId(), ex);
                }
            }
        }

        resultMap.put("total", notifyTaskEntityList.size());
        resultMap.put("success", successCount);
        resultMap.put("fail", failCount);
        resultMap.put("retry", retryCount);

        return resultMap;
    }
}

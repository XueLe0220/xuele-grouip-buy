package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.NotifyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 异步通知任务 DAO 接口
 * <p>
 * 对应表：notify_task
 * 职责：实现“本地消息表”模式，确保交易完成后，必定能通知到下游（发货/履约）。
 * 核心流程：
 * 1. 交易完成 -> 插入任务 (Insert)。
 * 2. 定时任务 -> 扫描未成功任务 (Query)。
 * 3. 执行回调 -> 更新结果与重试次数 (Update)。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/18
 */
@Mapper
public interface INotifyTaskDao {

    /**
     * 新增通知任务
     * <p>
     * 业务场景：
     * 在拼团状态更新为 COMPLETE 的同一事务中调用。
     * 初始状态通常为 0 (待处理)，重试次数为 0。
     *
     * @param notifyTask 通知任务 PO 对象
     */
    void insert(NotifyTask notifyTask);

    List<NotifyTask> queryUnExecutedNotifyTaskList();

    NotifyTask queryUnExecutedNotifyTaskByTeamId(String teamId);

    int updateNotifyTaskStatusSuccess(NotifyTask notifyTask);

    int updateNotifyTaskStatusError(NotifyTask notifyTask);

    int updateNotifyTaskStatusRetry(NotifyTask notifyTask);
}
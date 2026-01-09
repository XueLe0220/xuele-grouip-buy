package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拼团组队主表 DAO 接口
 * <p>
 * 对应表：group_buy_order
 * 职责：管理拼团的“团长/坑位”信息。
 * 核心功能：提供原子性的锁单操作，防止超卖。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/06 23:36
 */
@Mapper
public interface IGroupBuyOrderDao {

    /**
     * 查询拼团组队详情
     * <p>
     * 业务场景：
     * 1. 在锁单前，查询当前团是否存在、配置的目标人数是多少。
     * 2. 在前端展示拼团进度（还差几人成团）。
     *
     * @param groupBuyOrderReq 查询条件（主要是 teamId）
     * @return 组队详情 PO 对象
     */
    GroupBuyOrder queryGroupBuyProgress(GroupBuyOrder groupBuyOrderReq);

    /**
     * 原子性扣减库存（核心防超卖）
     * <p>
     * 对应的 SQL 逻辑：
     * UPDATE group_buy_order
     * SET lock_count = lock_count + 1
     * WHERE team_id = #{teamId}
     * AND (lock_count + complete_count) < target_count
     * <p>
     * 返回值含义：
     * 1：更新成功 -> 抢到了坑位，锁单成功。
     * 0：更新失败 -> 坑位已满（或 teamId 不存在），锁单失败。
     *
     * @param teamId 拼单组队ID
     * @return 受影响的行数 (1 或 0)
     */
    int updateAddLockCount(String teamId);

    /**
     * 插入新的拼团组队记录
     * <p>
     * 业务场景：
     * 当用户选择“发起拼单”（而不是参与别人的团）时，需要创建一个新的团。
     * 此时会生成一个新的 teamId，并初始化 lock_count = 0。
     *
     * @param groupBuyOrder 新团的 PO 对象
     */
    void insert(GroupBuyOrder groupBuyOrder);
}
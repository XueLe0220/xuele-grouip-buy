package cn.xuele.infrastructure.dao;

import cn.xuele.infrastructure.dao.po.GroupBuyActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拼团活动持久化层接口
 * <p>
 * 对应表：group_buy_activity
 * 职责：负责拼团活动规则、状态、时间的增删改查。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2025/12/24 17:14
 */
@Mapper
public interface IGroupBuyActivityDao {

    GroupBuyActivity queryValidGroupBuyActivity(GroupBuyActivity groupBuyActivityReq);

}


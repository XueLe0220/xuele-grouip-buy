package cn.xuele.domain.trade.adapter.port;

import cn.xuele.domain.trade.model.entity.NotifyTaskEntity;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/19 13:15
 */
public interface ITradePort {
    String groupBuyNotify(NotifyTaskEntity notifyTask);
}

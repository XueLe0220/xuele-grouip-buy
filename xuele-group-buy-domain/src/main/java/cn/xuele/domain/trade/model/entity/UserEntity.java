package cn.xuele.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体
 * <p>
 * 领域定义：
 * 代表交易的“发起者”。
 * 在交易上下文中，这是一个“轻量级”的用户对象，只携带交易链路必须的身份字段。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/07 18:20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    /** 用户ID */
    private String userId;

}
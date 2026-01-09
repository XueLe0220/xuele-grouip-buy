package cn.xuele.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易订单状态值对象
 * <p>
 * 将数据库中的 magic number (0, 1, 2) 映射为具有业务含义的枚举。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/08 14:50
 */
@Getter
@AllArgsConstructor
public enum TradeOrderStatusEnumVO {

    /**
     * 初始创建 (0)
     * <p>
     * 业务含义：锁单成功，订单已生成，库存/坑位已占用，但用户尚未支付。
     * 对应数据库 status = 0
     */
    CREATE(0, "初始创建"),

    /**
     * 消费完成 (1)
     * <p>
     * 业务含义：用户支付成功，交易闭环。
     * 对应数据库 status = 1
     */
    COMPLETE(1, "消费完成"),

    /**
     * 超时关单 (2)
     * <p>
     * 业务含义：在指定时间内（如15分钟）未支付，系统自动关闭订单并释放库存。
     * 对应数据库 status = 2
     */
    CLOSE(2, "超时关单"),
    ;

    private final Integer code;
    private final String info;

    /**
     * 根据状态码获取枚举对象
     *
     * @param code 数据库存储的状态码
     * @return 对应的枚举对象，匹配不到时默认返回 CREATE (或可抛出异常)
     */
    public static TradeOrderStatusEnumVO valueOf(Integer code) {
        if (code == null) {
            return CREATE;
        }
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> COMPLETE;
            case 2 -> CLOSE;
            default -> CREATE; // 兜底逻辑
        };
    }
}
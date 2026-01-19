package cn.xuele.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 拼团组队状态枚举 VO
 * <p>
 * 对应表：group_buy_team (拼团表) 的 status 字段。
 * 作用：描述一个“团”从创建到结束的生命周期状态。
 *
 * @author XueLe
 * @since 2026/01/18
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum GroupBuyTeamOrderVO {

    /** * 拼单中
     * <p>
     * 含义：团已开启，lockCount < targetCount，尚有名额，允许新用户加入。
     */
    PROGRESS(0, "拼单中"),

    /** * 拼单完成
     * <p>
     * 含义：团已满员 (lockCount >= targetCount) 且所有订单均已支付确认。
     * 此时不可再加入，流程结束，等待发货。
     */
    COMPLETE(1, "完成"),

    /** * 拼单失败
     * <p>
     * 含义：活动到期仍未达到目标人数，或团长主动取消。
     * 此时触发退款流程。
     */
    FAIL(2, "失败"),
    ;

    private Integer code;
    private String info;

    /**
     * 根据状态码获取枚举对象
     * * @param code 数据库存储的状态码
     * @return 对应的枚举对象
     */
    public static GroupBuyTeamOrderVO valueOf(Integer code) {
        return switch (code) {
            case 0 -> PROGRESS;
            case 1 -> COMPLETE;
            case 2 -> FAIL;
            default -> throw new RuntimeException("Err: GroupBuy status code [" + code + "] not exist!");
        };
    }
}
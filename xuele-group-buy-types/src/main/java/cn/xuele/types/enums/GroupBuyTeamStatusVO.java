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
public enum GroupBuyTeamStatusVO {

    PROGRESS(0, "拼单中"),
    COMPLETE(1, "完成"),
    FAIL(2, "失败"),
    ;

    private Integer code;
    private String info;

    /**
     * 根据状态码获取枚举对象
     * * @param code 数据库存储的状态码
     * @return 对应的枚举对象
     */
    public static GroupBuyTeamStatusVO valueOf(Integer code) {
        return switch (code) {
            case 0 -> PROGRESS;
            case 1 -> COMPLETE;
            case 2 -> FAIL;
            default -> throw new RuntimeException("Err: GroupBuy status code [" + code + "] not exist!");
        };
    }
}
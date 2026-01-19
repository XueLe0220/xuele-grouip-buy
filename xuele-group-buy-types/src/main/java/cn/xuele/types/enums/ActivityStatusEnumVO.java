package cn.xuele.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 营销活动状态枚举
 * <p>
 * 描述活动全生命周期的流转状态
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 11:52
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ActivityStatusEnumVO {

    /** [0] 已创建/草稿：活动配置已入库，但未到开始时间 */
    CREATE(0, "创建"),

    /** [1] 生效中/进行中：活动正在进行，允许用户参与 */
    EFFECTIVE(1, "生效"),

    /** [2] 已过期/结束：活动时间已截止，停止发券 */
    OVERDUE(2, "过期"),

    /** [3] 已废弃/下线：人工强制终止活动 */
    ABANDONED(3, "废弃"),
    ;

    private Integer code;
    private String info;

    /**
     * 根据状态码获取枚举对象
     */
    public static ActivityStatusEnumVO valueOf(Integer code) {
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> EFFECTIVE;
            case 2 -> OVERDUE;
            case 3 -> ABANDONED;
            default -> throw new RuntimeException("Activity Status Code not exist: " + code);
        };
    }
}
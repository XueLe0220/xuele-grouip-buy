package cn.xuele.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TODO: 类描述
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/17 11:52
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ActivityStatusEnumVO {

    CREATE(0, "创建"),
    EFFECTIVE(1, "生效"),
    OVERDUE(2, "过期"),
    ABANDONED(3, "废弃"),
    ;

    private Integer code;
    private String info;

    public static ActivityStatusEnumVO valueOf(Integer code) {
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> EFFECTIVE;
            case 2 -> OVERDUE;
            case 3 -> ABANDONED;
            default -> throw new RuntimeException("err code not exist!");
        };
    }

}
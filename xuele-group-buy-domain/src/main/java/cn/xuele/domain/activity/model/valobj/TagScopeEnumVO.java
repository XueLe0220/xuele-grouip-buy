package cn.xuele.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 人群标签判定标准 - 领域值对象 (Value Object)
 * <p>
 * 这里的 VO 后缀指代 Value Object（值对象），用于封装领域内的不可变状态判定标准，
 * 作为一个“度量衡”存在，而非 View Object（视图对象）。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/04 20:00
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum TagScopeEnumVO {

    /**
     * 可见性判定
     * allow=true: 默认可见 (未配置限制时)
     * refuse=false: 需要白名单校验 (配置了限制时)
     */
    VISIBLE(true, false, "是否可看见拼团"),

    /**
     * 参与性判定
     * allow=true: 默认可参与 (未配置限制时)
     * refuse=false: 需要白名单校验 (配置了限制时)
     */
    ENABLE(true, false, "是否可参与拼团"),
    ;

    /**
     * 允许放行 (当规则未命中/无限制时的默认行为)
     */
    private Boolean allow;

    /**
     * 拒绝/需校验 (当规则命中/有限制时的行为)
     */
    private Boolean refuse;

    /**
     * 描述信息
     */
    private String desc;
}
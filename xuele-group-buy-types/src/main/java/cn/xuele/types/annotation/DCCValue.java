package cn.xuele.types.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态配置中心值注入注解
 * <p>
 * 作用：
 * 用于标记在类的字段（Field）上。
 * 当 Spring 容器启动或配置发生变更时，DCC 框架会扫描带有此注解的字段，
 * 并根据 value() 指定的 Key，从 Redis 中获取最新值注入到该字段中。
 *
 * @author XueLe
 * @version 1.0.0
 * @since 2026/01/05 13:08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DCCValue {

    /**
     * 配置项的 Key (对应 Redis 中的 Key)
     * <p>
     * 例如：如果 Redis 中存的是 "dcc:downgradeSwitch"，这里可能填 "downgradeSwitch"
     * 具体前缀逻辑由解析器决定。
     *
     * @return 配置键名
     */
    String value() default "";
}
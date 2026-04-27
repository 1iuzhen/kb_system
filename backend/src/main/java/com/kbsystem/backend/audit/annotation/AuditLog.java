package com.kbsystem.backend.audit.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作动作。
     *
     * @return 动作值
     */
    String action();

    /**
     * 操作目标类型。
     *
     * @return 目标类型
     */
    String targetType();

    /**
     * 目标 ID 参数索引（方法参数下标）。
     * 小于 0 时表示不从参数中提取目标 ID。
     *
     * @return 参数索引
     */
    int targetIdArgIndex() default -1;
}


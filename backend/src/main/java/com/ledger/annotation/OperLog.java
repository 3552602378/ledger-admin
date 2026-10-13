package com.ledger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 操作日志注解：标注在 Controller 方法上，由 OperLogAspect 记录操作行为 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 模块标题，如"收支记录" */
    String title();

    /** 业务类型：1新增 2修改 3删除 4导出 5登录 */
    int businessType();
}

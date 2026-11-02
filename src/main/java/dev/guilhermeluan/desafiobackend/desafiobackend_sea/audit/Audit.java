package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

import java.lang.annotation.*;


@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    AuditAction action();

    String resourceId() default "";
}

package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidUfValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
public @interface ValidUf {

    String message() default "UF inválida: use a sigla de duas letras de um estado brasileiro";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUfValidator implements ConstraintValidator<ValidUf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Uf.isValid(value);
    }
}

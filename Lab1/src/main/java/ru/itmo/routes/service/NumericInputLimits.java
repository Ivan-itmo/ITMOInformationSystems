package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;

@ApplicationScoped
public class NumericInputLimits {
    public static final int max_length = 20;

    @Inject
    private Validator validator;

    public String checkedLiteral(String value) {
        var violations = validator.validate(new Literal(value));
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return value;
    }

    private record Literal(@Size(max = max_length, message = "Число должно содержать не более {max} символов") String value) { }
}

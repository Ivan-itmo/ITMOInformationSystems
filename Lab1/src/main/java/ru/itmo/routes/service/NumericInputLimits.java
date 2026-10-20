package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NumericInputLimits {
    public static final int max_length = 20;

    public String checkedLiteral(String value) {
        if (value != null && value.length() > max_length) {
            throw new ValidationException("Число должно содержать не более " + max_length + " символов");
        }
        return value;
    }

}

package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ParameterValidator {
    @Inject
    private NumericInputLimits numericLimits;

    public long positiveLong(String value, String name) {
        return boundedLong(value, name, 1, Long.MAX_VALUE);
    }

    public int nonNegativeInt(String value, String name) {
        return (int) boundedLong(value, name, 0, Integer.MAX_VALUE);
    }

    public int positiveInt(String value, String name) {
        return (int) boundedLong(value, name, 1, Integer.MAX_VALUE);
    }

    private long boundedLong(String value, String name, long minimum, long maximum) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Параметр " + name + " обязателен");
        }
        numericLimits.checkedLiteral(value);
        if (!value.matches("[0-9]+")) {
            throw new ValidationException("Параметр " + name + " должен содержать только цифры");
        }
        long number;
        try {
            number = Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Параметр " + name + " выходит за диапазон long");
        }
        if (number < minimum || number > maximum) {
            throw new ValidationException("Параметр " + name + " должен быть от " + minimum + " до " + maximum);
        }
        return number;
    }
}

package ru.itmo.routes.validation;

public final class NumericInputLimits {
    private static final int MAX_LENGTH = 20;

    private NumericInputLimits() {}

    public static String checkedLiteral(String value) {
        if (value.length() > MAX_LENGTH) {
            throw new ValidationException("Число должно содержать не более " + MAX_LENGTH + " символов");
        }
        return value;
    }

}

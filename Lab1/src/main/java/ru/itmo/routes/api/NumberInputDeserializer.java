package ru.itmo.routes.api;

import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import ru.itmo.routes.validation.NumericInputLimits;
import ru.itmo.routes.validation.ValidationException;

public class NumberInputDeserializer implements JsonbDeserializer<Number> {
    @Override
    public Number deserialize(JsonParser parser, DeserializationContext context, Type type) {
        String literal;
        try {
            literal = parser.getString();
        } catch (IllegalStateException exception) {
            if (parser.getValue().getValueType() == JsonValue.ValueType.NULL) return null;
            throw new ValidationException("Ожидалось число");
        }
        literal = NumericInputLimits.checkedLiteral(literal);
        if (type == Long.class) {
            try {
                return new BigDecimal(literal).longValueExact();
            } catch (NumberFormatException | ArithmeticException exception) {
                throw new ValidationException("Ожидалось целое число в диапазоне long");
            }
        }
        if (type == Double.class) {
            return parseDecimal(literal);
        }
        throw new JsonbException("Неподдерживаемый тип числа: " + type);
    }

    private double parseDecimal(String literal) {
        BigDecimal input;
        double value;
        try {
            input = new BigDecimal(literal.trim());
            value = Double.parseDouble(literal);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Ожидалось конечное десятичное число");
        }
        if (!Double.isFinite(value)) {
            throw new ValidationException("Число выходит за диапазон double");
        }

        // Compare at the input's significant precision, ignoring trailing zeroes.
        MathContext precision = new MathContext(input.stripTrailingZeros().precision(), RoundingMode.HALF_EVEN);
        if (new BigDecimal(value).round(precision).compareTo(input) != 0) {
            throw new ValidationException("Число теряет значащие цифры при преобразовании в double");
        }
        return value;
    }
}

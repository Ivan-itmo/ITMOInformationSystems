package ru.itmo.routes.api;

import jakarta.inject.Inject;
import jakarta.enterprise.context.Dependent;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import ru.itmo.routes.service.NumericInputLimits;

@Dependent
public class NumberInputDeserializer implements JsonbDeserializer<Number> {
    @Inject
    private NumericInputLimits limits;

    @Override
    public Number deserialize(JsonParser parser, DeserializationContext context, Type type) {
        String literal;
        try {
            literal = parser.getString();
        } catch (IllegalStateException exception) {
            if (parser.getValue().getValueType() == JsonValue.ValueType.NULL) return null;
            throw new JsonbException("Ожидалось число", exception);
        }
        literal = limits.checkedLiteral(literal);
        try {
            if (type == Long.class) return new BigDecimal(literal).longValueExact();
            if (type == Double.class) return Double.valueOf(literal);
            throw new JsonbException("Неподдерживаемый тип числа: " + type);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new JsonbException("Некорректное число для типа " + type, exception);
        }
    }
}

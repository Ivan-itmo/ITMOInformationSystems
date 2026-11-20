package ru.itmo.routes.api;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import ru.itmo.routes.dto.AddRouteBetweenLocationsRequest;
import ru.itmo.routes.dto.DeleteLocationRequest;
import ru.itmo.routes.dto.LocationRequest;
import ru.itmo.routes.dto.RouteRequest;
import ru.itmo.routes.validation.ValidationException;

@Provider
public class JsonRequestInterceptor implements ReaderInterceptor {
    private static final JsonParserFactory PARSERS = Json.createParserFactory(Map.of());

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        Class<?> type = context.getType();
        boolean route = type == RouteRequest.class || type == AddRouteBetweenLocationsRequest.class;
        if (!route && type != LocationRequest.class && type != DeleteLocationRequest.class) {
            return context.proceed();
        }

        try {
            validateFields(context, type, route);
        } catch (JsonException exception) {
            throw requestError(400, "Некорректный синтаксис JSON", exception);
        } catch (ValidationException exception) {
            throw requestError(422, exception.getMessage(), exception);
        }

        try {
            return context.proceed();
        } catch (RuntimeException exception) {
            boolean bindingError = false;
            Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof ValidationException) {
                    throw requestError(422, cause.getMessage(), exception);
                }
                if (cause instanceof JsonbException) {
                    bindingError = true;
                }
            }
            if (bindingError) {
                throw requestError(422, "Тип или формат поля запроса не соответствует ожидаемому", exception);
            }
            throw exception;
        }
    }

    private WebApplicationException requestError(int status, String message, RuntimeException cause) {
        return new WebApplicationException(message, cause, ErrorMapper.error(status, message));
    }

    private void validateFields(ReaderInterceptorContext context, Class<?> type, boolean route) throws IOException {
        byte[] body = context.getInputStream().readAllBytes();
        if (body.length == 0) throw new ValidationException("Тело запроса обязательно");
        JsonValue value;
        try (JsonParser parser = PARSERS.createParser(new ByteArrayInputStream(body), StandardCharsets.UTF_8)) {
            if (!parser.hasNext()) throw new ValidationException("Тело запроса обязательно");
            parser.next();
            value = parser.getValue();
            if (parser.hasNext()) {
                throw new JsonException("Ожидалось одно значение JSON");
            }
        }
        if (!(value instanceof JsonObject object)) {
            throw new ValidationException("Тело запроса должно быть JSON-объектом");
        }
        if (route) {
            requireFields(object, "name", "coordinates", "fromLocationId", "toLocationId", "distance", "rating");
            requireType(object, "name", JsonValue.ValueType.STRING);
            requireType(object, "coordinates", JsonValue.ValueType.OBJECT);
            if (object.get("coordinates") instanceof JsonObject coordinates) {
                requireFields(coordinates, "x", "y");
            }
        } else if (type == LocationRequest.class) {
            requireFields(object, "x", "y", "z");
        } else {
            requireFields(object, "replacementLocationId");
        }
        context.setInputStream(new ByteArrayInputStream(body));
    }

    private void requireFields(JsonObject object, String... fields) {
        for (String field : fields) {
            if (!object.containsKey(field)) {
                throw new ValidationException("В запросе отсутствует поле " + field);
            }
        }
    }

    private void requireType(JsonObject object, String field, JsonValue.ValueType type) {
        JsonValue.ValueType actual = object.get(field).getValueType();
        if (actual != type && actual != JsonValue.ValueType.NULL) {
            throw new ValidationException("Недопустимый тип поля " + field);
        }
    }
}

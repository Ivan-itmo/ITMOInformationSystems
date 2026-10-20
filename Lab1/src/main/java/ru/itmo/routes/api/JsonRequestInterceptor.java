package ru.itmo.routes.api;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;

@Provider
public class JsonRequestInterceptor implements ReaderInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        try {
            return context.proceed();
        } catch (RuntimeException exception) {
            boolean parsingError = false;
            java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof jakarta.validation.ValidationException
                        || cause instanceof ru.itmo.routes.service.ValidationException) {
                    throw exception;
                }
                if (cause instanceof JsonbException || cause instanceof JsonException) {
                    parsingError = true;
                }
            }
            if (parsingError) {
                throw new BadRequestException("Некорректный JSON или тип данных в запросе", exception);
            }
            throw exception;
        }
    }
}

package ru.itmo.routes.api;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.util.Map;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.ValidationException;

@Provider
public class ErrorMapper implements ExceptionMapper<RuntimeException>, ReaderInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        try {
            return context.proceed();
        } catch (RuntimeException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof JsonbException || cause instanceof JsonException) {
                    throw new BadRequestException("Некорректное тело запроса: проверьте синтаксис JSON и типы полей.", exception);
                }
            }
            throw exception;
        }
    }

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof ValidationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", exception.getMessage()))
                    .build();
        }
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", exception.getMessage()))
                    .build();
        }
        if (exception instanceof WebApplicationException webException) {
            int status = webException.getResponse().getStatus();
            return Response.status(status)
                    .entity(Map.of("message", status == 400
                            ? "Некорректные параметры запроса"
                            : "Не удалось выполнить запрос (HTTP " + status + ")"))
                    .build();
        }
        return Response.serverError()
                .entity(Map.of("message", rootMessage(exception)))
                .build();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return current.getClass().getSimpleName() + ": " + message;
    }
}

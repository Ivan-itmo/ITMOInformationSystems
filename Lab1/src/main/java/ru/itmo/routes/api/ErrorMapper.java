package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.ValidationException;

@Provider
public class ErrorMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException exception) {
        exception.printStackTrace();
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
                            ? "Некорректные параметры запроса. Проверьте значения и формат ID: требуется целое число больше 0."
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

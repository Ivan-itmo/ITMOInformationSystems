package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.validation.ConstraintViolationException;
import ru.itmo.routes.validation.NotFoundException;
import ru.itmo.routes.validation.ConflictException;
import ru.itmo.routes.validation.ValidationException;

@Provider
public class ErrorMapper implements ExceptionMapper<RuntimeException> {
    private static final Logger LOGGER = Logger.getLogger(ErrorMapper.class.getName());

    @Override
    public Response toResponse(RuntimeException exception) {
        java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Response webResponse = null;
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof ValidationException || cause instanceof ConstraintViolationException) {
                return error(422, cause.getMessage());
            }
            if (cause instanceof NotFoundException) {
                return error(404, cause.getMessage());
            }
            if (cause instanceof ConflictException) {
                return error(409, cause.getMessage());
            }
            if (cause instanceof WebApplicationException webException) {
                if (webResponse == null) webResponse = webException.getResponse();
            }
        }
        if (webResponse != null) {
            if (webResponse.getStatus() >= 500) {
                LOGGER.log(Level.SEVERE, "HTTP " + webResponse.getStatus() + " при обработке REST-запроса", exception);
            }
            String message = webResponse.getStatus() < 500
                    ? exception.getMessage() : "Внутренняя ошибка сервера";
            return Response.fromResponse(webResponse).type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("message", message == null ? "Ошибка запроса" : message)).build();
        }
        LOGGER.log(Level.SEVERE, "HTTP 500: необработанное исключение при обработке REST-запроса", exception);
        return error(500, "Внутренняя ошибка сервера");
    }

    static Response error(int status, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message == null ? "Ошибка запроса" : message)).build();
    }
}

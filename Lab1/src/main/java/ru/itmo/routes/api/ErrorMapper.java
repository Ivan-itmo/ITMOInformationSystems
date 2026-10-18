package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.validation.ConstraintViolationException;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.ConflictException;
import ru.itmo.routes.service.ValidationException;

@Provider
public class ErrorMapper implements ExceptionMapper<RuntimeException> {
    private static final Logger LOGGER = Logger.getLogger(ErrorMapper.class.getName());

    @Override
    public Response toResponse(RuntimeException exception) {
        java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Response webResponse = null;
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof ValidationException || cause instanceof ConstraintViolationException) {
                return Response.status(422).build();
            }
            if (cause instanceof NotFoundException) {
                return Response.status(404).build();
            }
            if (cause instanceof ConflictException) {
                return Response.status(409).build();
            }
            if (cause instanceof WebApplicationException webException) {
                if (webResponse == null) webResponse = webException.getResponse();
            }
        }
        if (webResponse != null) {
            if (webResponse.getStatus() >= 500) {
                LOGGER.log(Level.SEVERE, "HTTP " + webResponse.getStatus() + " при обработке REST-запроса", exception);
            }
            return webResponse;
        }
        LOGGER.log(Level.SEVERE, "HTTP 500: необработанное исключение при обработке REST-запроса", exception);
        return Response.status(500).build();
    }
}

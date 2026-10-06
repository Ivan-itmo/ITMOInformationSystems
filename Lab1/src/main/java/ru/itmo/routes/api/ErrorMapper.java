package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.service.NotFoundException;
import ru.itmo.routes.service.ValidationException;

@Provider
public class ErrorMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof ValidationException) {
            return Response.status(400).build();
        }
        if (exception instanceof NotFoundException) {
            return Response.status(404).build();
        }
        if (exception instanceof WebApplicationException webException) {
            return Response.status(webException.getResponse().getStatus()).build();
        }
        return Response.status(500).build();
    }
}

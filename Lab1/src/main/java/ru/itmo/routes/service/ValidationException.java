package ru.itmo.routes.service;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

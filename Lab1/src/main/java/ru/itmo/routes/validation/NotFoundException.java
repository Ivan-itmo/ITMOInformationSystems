package ru.itmo.routes.validation;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
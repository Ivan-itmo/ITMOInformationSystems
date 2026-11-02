package ru.itmo.routes.validation;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

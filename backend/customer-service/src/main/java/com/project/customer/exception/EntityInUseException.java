package com.project.customer.exception;

public class EntityInUseException extends RuntimeException {
    public EntityInUseException(String message) {
        super(message);
    }
}

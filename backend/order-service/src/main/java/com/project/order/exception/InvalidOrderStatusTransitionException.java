package com.project.order.exception;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}

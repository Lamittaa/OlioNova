package com.project.order.exception;

public class InvalidOrderItemsException extends RuntimeException {

    public InvalidOrderItemsException(String message) {
        super(message);
    }
}

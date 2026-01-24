package com.project.order.exception;

public class OrderNotEditableException extends RuntimeException {
    public OrderNotEditableException(String message) {
        super(message);
    }
}

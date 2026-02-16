package com.project.queue_service.exception;

public class NoWaitingTicketException extends RuntimeException {
    public NoWaitingTicketException(String message) {
        super(message);
    }
}
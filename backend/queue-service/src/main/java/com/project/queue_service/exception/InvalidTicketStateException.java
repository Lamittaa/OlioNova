package com.project.queue_service.exception;

public class InvalidTicketStateException extends RuntimeException {

    private final String errorCode = "INVALID_TICKET_STATE";

    public InvalidTicketStateException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
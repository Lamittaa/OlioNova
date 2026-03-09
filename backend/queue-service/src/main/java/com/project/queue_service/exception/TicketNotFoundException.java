package com.project.queue_service.exception;

public class TicketNotFoundException extends RuntimeException {

    private final String errorCode = "TICKET_NOT_FOUND";

    public TicketNotFoundException(Long id) {
        super("Ticket not found with id: " + id);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
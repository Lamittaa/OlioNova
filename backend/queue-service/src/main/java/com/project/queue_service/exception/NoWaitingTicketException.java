package com.project.queue_service.exception;

import com.project.queue_service.model.QueueType;

public class NoWaitingTicketException extends RuntimeException {

    private final String errorCode = "NO_WAITING_TICKET";

    public NoWaitingTicketException(QueueType queueType) {
        super("No waiting tickets in queue: " + queueType);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
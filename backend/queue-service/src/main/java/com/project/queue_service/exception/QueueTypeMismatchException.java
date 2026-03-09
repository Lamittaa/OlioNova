package com.project.queue_service.exception;
public class QueueTypeMismatchException extends RuntimeException {

    private final String errorCode = "QUEUE_TYPE_MISMATCH";

    public QueueTypeMismatchException(String queueType) {
        super("Queue type mismatch for: " + queueType);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
package com.project.queue_service.exception;

public class ExternalServiceException extends RuntimeException {

    private final String errorCode = "EXTERNAL_SERVICE_ERROR";

    public ExternalServiceException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
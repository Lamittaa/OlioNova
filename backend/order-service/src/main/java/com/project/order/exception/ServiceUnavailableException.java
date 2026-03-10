package com.project.order.exception;



public class ServiceUnavailableException extends RuntimeException {

    private final String errorCode;

    public ServiceUnavailableException(String message) {
        super(message);
        this.errorCode = "SERVICE_UNAVAILABLE";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
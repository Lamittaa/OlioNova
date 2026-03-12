package com.project.productionStages.exception;

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends RuntimeException {

    private final String errorCode;

    public ServiceUnavailableException(String message) {
        super(message);
        this.errorCode = "SERVICE_UNAVAILABLE";
    }

    public ServiceUnavailableException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
package com.zaytoun.aiprediction.exception;

public class ColdStartException extends RuntimeException {
    public ColdStartException(String message) {
        super(message);
    }
}

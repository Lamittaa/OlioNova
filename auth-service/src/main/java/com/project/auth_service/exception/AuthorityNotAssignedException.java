package com.project.auth_service.exception;

public class AuthorityNotAssignedException extends RuntimeException {
    public AuthorityNotAssignedException(String message) {
        super(message);
    }
}


package com.project.auth_service.exception;

public class TokenRevokedException extends RuntimeException {
    public TokenRevokedException(String msg) { super(msg); }
}

package com.project.auth_service.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String msg) { super(msg); }
}

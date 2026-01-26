package com.project.customer.exception;

import com.project.customer.dto.ErrorResponse;
import com.project.customer.dto.FieldErrorDto;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- 404 ----------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "RESOURCE_NOT_FOUND", null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        log.warn("[EXCEPTION] Entity not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "ENTITY_NOT_FOUND", null);
    }
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, "Unauthorized", req, "UNAUTHORIZED", null);
}
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "Forbidden", req, "FORBIDDEN", null);
}
    // ---------- 409 ----------
    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlreadyExists(EntityAlreadyExistsException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, "ENTITY_ALREADY_EXISTS", null);
    }

    // ---------- 400 ----------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "BAD_REQUEST", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDto)
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, "VALIDATION_ERROR", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldErrorDto> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDto(
                        v.getPropertyPath().toString(),
                        v.getMessage(),
                        v.getInvalidValue()
                ))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, "VALIDATION_ERROR", fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "MISSING_PARAMETER", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParse(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req, "MALFORMED_JSON", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, "METHOD_NOT_ALLOWED", null);
    }

    // ---------- 5xx عام ----------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]", traceId, ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error. If this persists, contact support with traceId=" + traceId,
                req,
                "INTERNAL_ERROR",
                null
        );
    }

    @ExceptionHandler(EntityInUseException.class)
    public ResponseEntity<ErrorResponse> handleEntityInUse(EntityInUseException ex, HttpServletRequest req) {
        log.warn("Attempted to delete an entity which is in use: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, "Entity_IN_USE", null);
    }

    // ---------- Helpers ----------
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String msg, HttpServletRequest req,
                                               String code, List<FieldErrorDto> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(msg)
                .path(req.getRequestURI())
                .code(code)
                .errors(fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private FieldErrorDto toFieldErrorDto(FieldError fe) {
        return new FieldErrorDto(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue());
    }
}

package com.project.productionStages.exception;

import com.project.productionStages.dto.ErrorResponse;
import com.project.productionStages.dto.FieldErrorDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // 🔴 NOT FOUND
    // =========================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                "RESOURCE_NOT_FOUND",
                null
        );
    }

    // =========================================================
    // 🔴 BUSINESS RULE
    // =========================================================
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessRuleException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                ex.getErrorCode(), // 🔥 مهم
                null
        );
    }

    // =========================================================
    // 🔴 VALIDATION (BODY)
    // =========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                "VALIDATION_ERROR",
                fieldErrors
        );
    }

    // =========================================================
    // 🔴 VALIDATION (PARAMS)
    // =========================================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        List<FieldErrorDto> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> new FieldErrorDto(
                        v.getPropertyPath().toString(),
                        v.getMessage(),
                        v.getInvalidValue()
                ))
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                "VALIDATION_ERROR",
                fieldErrors
        );
    }

    // =========================================================
    // 🔴 METHOD NOT ALLOWED
    // =========================================================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMessage(),
                request,
                "METHOD_NOT_ALLOWED",
                null
        );
    }

    // =========================================================
    // 🔴 SERVICE DOWN (Feign / Queue / Order)
    // =========================================================
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                request,
                ex.getErrorCode(),
                null
        );
    }

    // =========================================================
    // 🔴 FALLBACK ERROR
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest request
    ) {

        String traceId = UUID.randomUUID().toString();

        log.error("Unhandled exception [traceId={}]", traceId, ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error (traceId=" + traceId + ")",
                request,
                "INTERNAL_ERROR",
                null
        );
    }

    // =========================================================
    // 🟢 BUILDER (ثيم موحد)
    // =========================================================
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            String code,
            List<FieldErrorDto> errors
    ) {

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .code(code)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    // =========================================================
    // 🟢 FIELD MAPPER
    // =========================================================
    private FieldErrorDto mapFieldError(FieldError fe) {
        return new FieldErrorDto(
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getRejectedValue()
        );
    }
}
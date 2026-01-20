package com.project.order.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.project.order.dto.ErrorResponse;
import com.project.order.dto.FieldErrorDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, "METHOD_NOT_ALLOWED", null);
    }

    // ---------- 409 (DB constraints) ----------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {

        String msg = "Database constraint violation. The request conflicts with existing data.";

        String traceId = UUID.randomUUID().toString();
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("DataIntegrityViolationException [traceId={}]: {}", traceId, detail, ex);

        return build(
                HttpStatus.CONFLICT,
                msg + " traceId=" + traceId,
                req,
                "DATA_INTEGRITY_VIOLATION",
                null
        );
    }

    // ---------- 400 (JSON parsing / mapping) ----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {

        // 1) Body مش موجود
        String rawMsg = ex.getMessage() == null ? "" : ex.getMessage();
        if (rawMsg.contains("Required request body is missing")) {
            return build(HttpStatus.BAD_REQUEST, "Request body is required", req, "REQUEST_BODY_MISSING", null);
        }

        // الأهم: هاد بيجيب السبب الحقيقي غالبًا
        Throwable cause = ex.getMostSpecificCause();

        // 2) JSON Syntax غلط
        if (cause instanceof JsonParseException jpe) {
            String msg = "Malformed JSON. Check syntax near line " + jpe.getLocation().getLineNr()
                    + ", column " + jpe.getLocation().getColumnNr();
            return build(HttpStatus.BAD_REQUEST, msg, req, "MALFORMED_JSON", null);
        }

        // 3) Field اسمه غلط
        if (cause instanceof UnrecognizedPropertyException upe) {
            String field = upe.getPropertyName();
            List<FieldErrorDto> errors = List.of(new FieldErrorDto(field, "Unknown field in JSON", null));
            return build(HttpStatus.BAD_REQUEST, "Unknown field: " + field, req, "UNKNOWN_FIELD", errors);
        }

        // 4) نوع البيانات غلط
        if (cause instanceof InvalidFormatException ife) {
            String fieldPath = toPath(ife);
            Object badValue = ife.getValue();
            String targetType = ife.getTargetType() == null ? "unknown" : ife.getTargetType().getSimpleName();

            List<FieldErrorDto> errors = List.of(
                    new FieldErrorDto(fieldPath, "Invalid value type. Expected: " + targetType, badValue)
            );

            return build(
                    HttpStatus.BAD_REQUEST,
                    "Invalid value for field: " + fieldPath,
                    req,
                    "INVALID_FORMAT",
                    errors
            );
        }

        // 5) Structure مش compatible
        if (cause instanceof MismatchedInputException mie) {
            String fieldPath = toPath(mie);
            String msg = "JSON structure is not compatible with request DTO";
            List<FieldErrorDto> errors = fieldPath.isBlank()
                    ? null
                    : List.of(new FieldErrorDto(fieldPath, msg, null));

            return build(HttpStatus.BAD_REQUEST, msg, req, "MISMATCHED_INPUT", errors);
        }

        // default
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req, "MALFORMED_JSON", null);
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

    private String toPath(MismatchedInputException ex) {
        if (ex.getPath() == null || ex.getPath().isEmpty()) return "";
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .reduce((a, b) -> a + "." + b)
                .orElse("");
    }
}

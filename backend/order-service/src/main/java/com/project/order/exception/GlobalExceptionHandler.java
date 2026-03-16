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
    @ExceptionHandler(InvalidOrderItemsException.class)
public ResponseEntity<ErrorResponse> handleInvalidOrderItems(
        InvalidOrderItemsException ex,
        HttpServletRequest request
) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                    .status(400)
                    .error("Bad Request")
                    .code("INVALID_ORDER_ITEMS")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .build());
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
    @ExceptionHandler(CustomerNotFoundException.class)
public ResponseEntity<ErrorResponse> handleCustomerNotFound(
        CustomerNotFoundException ex,
        HttpServletRequest request) {

    return ResponseEntity.status(404)
            .body(ErrorResponse.builder()
                    .code("CUSTOMER_NOT_FOUND")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .status(404)
                    .build());
}


    // ---------- 400 (JSON parsing / mapping) ----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {

        String rawMsg = ex.getMessage() == null ? "" : ex.getMessage();
        if (rawMsg.contains("Required request body is missing")) {
            return build(HttpStatus.BAD_REQUEST, "Request body is required", req, "REQUEST_BODY_MISSING", null);
        }

        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof JsonParseException jpe) {
            String msg = "Malformed JSON. Check syntax near line " + jpe.getLocation().getLineNr()
                    + ", column " + jpe.getLocation().getColumnNr();
            return build(HttpStatus.BAD_REQUEST, msg, req, "MALFORMED_JSON", null);
        }

        if (cause instanceof UnrecognizedPropertyException upe) {
            String field = upe.getPropertyName();
            List<FieldErrorDto> errors = List.of(new FieldErrorDto(field, "Unknown field in JSON", null));
            return build(HttpStatus.BAD_REQUEST, "Unknown field: " + field, req, "UNKNOWN_FIELD", errors);
        }

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

        if (cause instanceof MismatchedInputException mie) {
            String fieldPath = toPath(mie);
            String msg = "JSON structure is not compatible with request DTO";
            List<FieldErrorDto> errors = fieldPath.isBlank()
                    ? null
                    : List.of(new FieldErrorDto(fieldPath, msg, null));

            return build(HttpStatus.BAD_REQUEST, msg, req, "MISMATCHED_INPUT", errors);
        }

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

    @ExceptionHandler(ServiceUnavailableException.class)
public ResponseEntity<ErrorResponse> handleServiceUnavailable(
        ServiceUnavailableException ex,
        HttpServletRequest request
) {

    return buildError(
            HttpStatus.SERVICE_UNAVAILABLE,
            request,
            ex.getErrorCode().toString(),
            ex.getMessage(),
            null
    );
}
private ResponseEntity<ErrorResponse> buildError(
        HttpStatus status,
        HttpServletRequest request,
        String code,
        String message,
        List<FieldErrorDto> fieldErrors
) {

    ErrorResponse body = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .code(code)
            .errors(fieldErrors)
            .build();

    return new ResponseEntity<>(body, status);
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

@ExceptionHandler(OrderNotEditableException.class)
public ResponseEntity<ErrorResponse> handleOrderNotEditable(
        OrderNotEditableException ex,
        HttpServletRequest request
) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                    .status(409)
                    .error("Conflict")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .code("ORDER_NOT_EDITABLE")
                    .build());
}
@ExceptionHandler(InvalidOrderStatusTransitionException.class)
public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
        InvalidOrderStatusTransitionException ex,
        HttpServletRequest request
) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                    .status(409)
                    .error("Conflict")
                    .code("INVALID_STATUS_TRANSITION")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .build());
}
@ExceptionHandler(OutOfStockException.class)
public ResponseEntity<ErrorResponse> handleOutOfStock(
        OutOfStockException ex,
        HttpServletRequest req) {

    return build(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            req,
            "OUT_OF_STOCK",
            null);
}
@ExceptionHandler(BusinessRuleViolationException.class)
public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
        BusinessRuleViolationException ex,
        HttpServletRequest request
) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                    .status(409)
                    .error("Conflict")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .code("BUSINESS_RULE_VIOLATION")
                    .build());
}


}

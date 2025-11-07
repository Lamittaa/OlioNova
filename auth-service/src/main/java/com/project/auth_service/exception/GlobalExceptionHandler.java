package com.project.auth_service.exception;

import com.project.auth_service.dto.ErrorResponseDto;
import com.project.auth_service.dto.FieldErrorDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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



    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleRoleNotFound(RoleNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "ROLE_NOT_FOUND", null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(UserNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "USER_NOT_FOUND", null);
    }

 

@ExceptionHandler({ BadCredentialsException.class, AuthenticationException.class })
public ResponseEntity<ErrorResponseDto> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    log.warn("Authentication failed from IP={} - {}", req.getRemoteAddr(), ex.getMessage());
    return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", req, "BAD_CREDENTIALS", null);
}
 
@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ErrorResponseDto> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
    log.warn("[EXCEPTION] Entity not found: {}", ex.getMessage());
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, "ENTITY_NOT_FOUND", null);
}
@ExceptionHandler(RoleInUseException.class)
public ResponseEntity<ErrorResponseDto> handleRoleInUse(RoleInUseException ex, HttpServletRequest req) {
    log.warn("Attempted to delete a role in use: {}", ex.getMessage());
    return build(HttpStatus.CONFLICT, ex.getMessage(), req, "ROLE_IN_USE", null);
}



    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Access is denied", req, "ACCESS_DENIED", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "BAD_REQUEST", null);
    }

   

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDto> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDto)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, "VALIDATION_ERROR", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldErrorDto> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDto(v.getPropertyPath().toString(), v.getMessage(), v.getInvalidValue()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, "VALIDATION_ERROR", fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "MISSING_PARAMETER", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleJsonParse(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req, "MALFORMED_JSON", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, "METHOD_NOT_ALLOWED", null);
    }

    // ---------- 5xx عام ----------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnknown(Exception ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        // نسجل الخطأ مع traceId للمراجعة
        log.error("Unhandled exception [traceId={}]", traceId, ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error. If this persists, contact support with traceId=" + traceId,
                req,
                "INTERNAL_ERROR",
                null);
    }



@ExceptionHandler(AuthorityNotAssignedException.class)
public ResponseEntity<ErrorResponseDto> handleAuthorityNotAssignedException(AuthorityNotAssignedException ex, HttpServletRequest req) {
    log.warn("Authority not assigned: {}", ex.getMessage());
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "AUTHORITY_NOT_ASSIGNED", null);
}



       // 🔹 Refresh Token Errors
    @ExceptionHandler({ InvalidTokenException.class })
    public ResponseEntity<ErrorResponseDto> handleInvalidRefresh(InvalidTokenException ex, HttpServletRequest req) {
        log.warn("Invalid refresh token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, "REFRESH_TOKEN_INVALID", null);
    }

    @ExceptionHandler({ TokenExpiredException.class })
    public ResponseEntity<ErrorResponseDto> handleRefreshExpired(TokenExpiredException ex, HttpServletRequest req) {
        log.warn("Expired refresh token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, "REFRESH_TOKEN_EXPIRED", null);
    }

    @ExceptionHandler({ TokenRevokedException.class })
    public ResponseEntity<ErrorResponseDto> handleRefreshRevoked(TokenRevokedException ex, HttpServletRequest req) {
        log.warn("Revoked refresh token used: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, "REFRESH_TOKEN_REVOKED", null);
    }

@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
    log.warn("Illegal state: {}", ex.getMessage());
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, "LOGOUT_ERROR", null);
}


@ExceptionHandler(EntityAlreadyExistsException.class)
public ResponseEntity<ErrorResponseDto> handleEntityAlreadyExists(EntityAlreadyExistsException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), req, "ENTITY_ALREADY_EXISTS", null);
}

    // ---------- Helpers ----------
    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String msg, HttpServletRequest req,
                                                String code, List<FieldErrorDto> fieldErrors) {
        ErrorResponseDto body = ErrorResponseDto.builder()
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

package com.project.queue_service.exception;

import com.project.queue_service.dto.ErrorResponse;
import com.project.queue_service.dto.FieldErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  
    @ExceptionHandler(NoWaitingTicketException.class)
    public ResponseEntity<ErrorResponse> handleNoWaiting(
            NoWaitingTicketException ex,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                request,
                "NO_WAITING_TICKET",
                ex.getMessage(),
                null
        );
    }

 
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                request,
                "INVALID_ARGUMENT",
                ex.getMessage(),
                null
        );
    }

  
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<FieldErrorDto> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> new FieldErrorDto(
                                err.getField(),
                                err.getDefaultMessage(),
                                err.getRejectedValue()
                        ))
                        .collect(Collectors.toList());

        return buildError(
                HttpStatus.BAD_REQUEST,
                request,
                "VALIDATION_ERROR",
                "Validation failed",
                errors
        );
    }

  
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.CONFLICT,
                request,
                "DATABASE_CONFLICT",
                "Database constraint violation",
                null
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                "INTERNAL_ERROR",
                ex.getMessage(),
                null
        );
    }
@ExceptionHandler(TicketNotFoundException.class)
public ResponseEntity<ErrorResponse> handleTicketNotFound(
        TicketNotFoundException ex,
        HttpServletRequest request
) {
    return buildError(
            HttpStatus.NOT_FOUND,
            request,
            ex.getErrorCode(),
            ex.getMessage(),
            null
    );
}

@ExceptionHandler(QueueTypeMismatchException.class)
public ResponseEntity<ErrorResponse> handleQueueTypeMismatch(
        QueueTypeMismatchException ex,
        HttpServletRequest request
) {
    return buildError(
            HttpStatus.BAD_REQUEST,
            request,
            ex.getErrorCode(),
            ex.getMessage(),
            null
    );
}  
@ExceptionHandler(ExternalServiceException.class)
public ResponseEntity<ErrorResponse> handleExternalService(
        ExternalServiceException ex,
        HttpServletRequest request
) {
    return buildError(
            HttpStatus.SERVICE_UNAVAILABLE,
            request,
            ex.getErrorCode(),
            ex.getMessage(),
            null
    );
}

@ExceptionHandler(InvalidTicketStateException.class)
public ResponseEntity<ErrorResponse> handleInvalidState(
        InvalidTicketStateException ex,
        HttpServletRequest request
) {
    return buildError(
            HttpStatus.BAD_REQUEST,
            request,
            ex.getErrorCode(),
            ex.getMessage(),
            null
    );
}


@ExceptionHandler(DuplicateTicketException.class)
public ResponseEntity<ErrorResponse> handleDuplicateTicket(
        DuplicateTicketException ex,
        HttpServletRequest request
) {

    return buildError(
            HttpStatus.CONFLICT,
            request,
            "DUPLICATE_TICKET",
            ex.getMessage(),
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
}
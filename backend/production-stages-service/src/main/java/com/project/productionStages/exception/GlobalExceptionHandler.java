package com.project.productionStages.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================================
    // Resource Not Found
    // ================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex){

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "error", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    // ================================
    // Business Rule Error
    // ================================
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<?> handleBusinessRule(BusinessRuleException ex){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "error", "BUSINESS_RULE_ERROR",
                        "message", ex.getMessage()
                ));
    }

    // ================================
    // General Error
    // ================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex){

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "error", "SERVER_ERROR",
                        "message", ex.getMessage()
                ));
    }

}
package com.project.queue_service.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String code;
    private List<FieldErrorDto> errors;
}

package com.project.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldErrorDto {
    private final String field;
    private final String message;
    private final Object rejectedValue;
}

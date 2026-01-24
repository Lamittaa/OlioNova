package com.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FieldErrorDto {
    private String field;
    private String message;
    private Object rejectedValue;
}

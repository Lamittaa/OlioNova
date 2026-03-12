package com.project.productionStages.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FieldErrorDto {

    private String field;

    private String message;

    private Object rejectedValue;

}
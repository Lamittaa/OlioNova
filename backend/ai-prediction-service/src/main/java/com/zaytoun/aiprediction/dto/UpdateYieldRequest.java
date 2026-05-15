package com.zaytoun.aiprediction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateYieldRequest {
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double actualYieldPercent;
}

package com.project.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecreaseAvailabilityRequest {

    @NotNull
    @Min(value = 1)
    private Integer quantity;
}
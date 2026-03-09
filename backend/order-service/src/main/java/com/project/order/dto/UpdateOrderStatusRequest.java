package com.project.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status cannot be empty")
    @Pattern(
        regexp = "^(SUBMITTED|READY_FOR_PAYMENT|PAID|IN_PRODUCTION|IN_PROGRESS|READY_FOR_PICKUP|COMPLETED|CANCELED|REFUNDED)$",
        message = "Invalid status value"
    )
    private String status;
}
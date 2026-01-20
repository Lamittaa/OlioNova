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
  regexp = "^(SUBMITTED|READY_FOR_PAYMENT|PAID|IN_PROGRESS|COMPLETED|CANCELED)$",
  message = "Status must be one of: SUBMITTED, READY_FOR_PAYMENT, PAID, IN_PROGRESS, COMPLETED, CANCELED"
)
    private String status;
}

package com.project.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreatePaymentRequest {

    @NotNull
    private Long orderId;
}

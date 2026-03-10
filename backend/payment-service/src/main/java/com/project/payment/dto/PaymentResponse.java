package com.project.payment.dto;

import com.project.payment.model.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal totalPrice;
    private PaymentType paymentType;
    private LocalDateTime paymentDate;
    private Long userId;
}

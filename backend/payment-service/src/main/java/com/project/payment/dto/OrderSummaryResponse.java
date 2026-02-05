package com.project.payment.dto;

import java.math.BigDecimal;

public record OrderSummaryResponse(
        Long id,
        BigDecimal totalPrice,
        String status
) {}

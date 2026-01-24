package com.project.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public class UpdateOrderRequest {
  @Positive
    private BigDecimal quantity;

    // فقط لخدمة العصر
    private String oliveType;
    private Integer bagsCount;
    private String note;
}

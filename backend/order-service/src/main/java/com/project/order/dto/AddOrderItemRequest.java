package com.project.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddOrderItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    // خاص بالعصر فقط
    private String oliveType;
    private Integer bagsCount;

    private String note;
}

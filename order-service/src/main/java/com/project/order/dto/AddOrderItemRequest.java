package com.project.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddOrderItemRequest {

    @NotNull(message = "Product ID cannot be null")
    @Min(value = 1, message = "Product ID must be greater than 0")
    private Long productId;

    @NotNull(message = "Quantity cannot be null")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Size(max = 50, message = "Olive type must be at most 50 characters")
    private String oliveType;

    @Min(value = 0, message = "Bags count cannot be negative")
    private Integer bagsCount;

    @Size(max = 255, message = "Note must be at most 255 characters")
    private String note;
}

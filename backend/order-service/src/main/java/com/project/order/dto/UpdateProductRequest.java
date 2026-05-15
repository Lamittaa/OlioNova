package com.project.order.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProductRequest {

    @Size(max = 100, message = "Product name must be at most 100 characters")
    private String productName;

    private String productType;

    @Min(value = 0, message = "Inventory total quantity cannot be negative")
    private Integer inventoryTotalQuantity;

    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 8 digits and 2 decimals")
    private BigDecimal price;

    private String unit;
}

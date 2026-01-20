package com.project.order.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProductRequest {

    @NotBlank(message = "Product name cannot be empty")
    @Size(max = 100, message = "Product name must be at most 100 characters")
    @Pattern(
        regexp = "^[\\p{L}0-9]+(?:[\\s\\-_/]+[\\p{L}0-9]+)*$",
        message = "Product name may contain letters/numbers and separators (space, -, _, /)"
    )
    private String productName;

    @NotBlank(message = "Product type cannot be empty")
    @Pattern(
        regexp = "(?i)^(OLIVE|JIFT|GALLON)$",
        message = "Product type must be one of: OLIVE, JIFT, GALLON"
    )
    private String productType;

    @Min(value = 0, message = "Inventory cannot be negative")
    private Integer inventory;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 8 digits and 2 decimals")
    private BigDecimal price;

    @NotBlank(message = "Unit cannot be empty")
    @Pattern(
        regexp = "(?i)^(KG|PCS|LITER)$",
        message = "Unit must be one of: KG, PCS, LITER"
    )
    private String unit;
}

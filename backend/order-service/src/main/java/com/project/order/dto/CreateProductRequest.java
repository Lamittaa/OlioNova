package com.project.order.dto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Product name cannot be empty")
    @Size(max = 100, message = "Product name must be at most 100 characters")
    private String productName;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 8 digits and 2 decimals")
    private BigDecimal price;

    @NotBlank(message = "Unit cannot be empty")
    private String unit;

    @NotNull(message = "Inventory is required")
    @Min(value = 0, message = "Inventory cannot be negative")
    private Integer inventoryTotalQuantity;
}
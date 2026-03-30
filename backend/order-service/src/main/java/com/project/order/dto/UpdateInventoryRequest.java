package com.project.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInventoryRequest {

    @NotNull(message = "Inventory total quantity cannot be null")
    @Min(value = 0, message = "Inventory total quantity cannot be negative")
    private Integer inventoryTotalQuantity;

    
}
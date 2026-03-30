package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryResponse {

    private Integer inventoryTotalQuantity;
    private Integer inventoryAvailabilityQuantity;
}
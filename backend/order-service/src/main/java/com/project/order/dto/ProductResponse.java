package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductResponse {

    private Long id;
    private String productName;
    private String productType;
    private Integer inventoryTotalQuantity;
    private Integer inventoryAvailabilityQuantity;
    private BigDecimal price;
    private String unit;
}
package com.project.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductResponse {

    private Long       id;
    private String     productName;
    private String     productType;
    private String     unit;
    private BigDecimal price;
    private Integer    inventoryTotalQuantity;
    private Integer    inventoryAvailabilityQuantity;
    private boolean    active;
}
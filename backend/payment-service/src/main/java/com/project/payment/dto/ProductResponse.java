package com.project.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductResponse {

    private Long       id;
    private String     productName;
    private String     productType;  // SERVICE / PURCHASE
    private String     unit;         // KG / PIECE  ← هذا الناقص
    private BigDecimal price;
    private Integer    inventory;    // null = غير محدود (SERVICE)
    private boolean    active;
}
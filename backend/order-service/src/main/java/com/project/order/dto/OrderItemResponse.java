package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public abstract class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productType;
    private BigDecimal quantity;
    private String status;   
}
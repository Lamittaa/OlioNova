package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {

    private Long id;

    private Long productId;
    private String productName;
    private String productType;

    private BigDecimal quantity;

    private BigDecimal price;       // snapshot (4 أو 6 للزيتون حسب العضوية)
    private BigDecimal totalPrice;  // price * quantity

    private String oliveType;
    private Integer bagsCount;
    private String note;
}

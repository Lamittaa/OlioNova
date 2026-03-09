package com.project.queue_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productType;
    private BigDecimal quantity;
    
    private BigDecimal price;
       private String status;   // 🔥 هذا ناقص عندك

    private BigDecimal totalPrice;
}
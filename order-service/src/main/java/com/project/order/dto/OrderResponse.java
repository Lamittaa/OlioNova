package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderResponse {

    private Long id;
    private Long customerId;

    private String status;

    private BigDecimal totalPrice;

    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

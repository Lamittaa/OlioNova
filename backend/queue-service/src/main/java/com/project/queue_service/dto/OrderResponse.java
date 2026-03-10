package com.project.queue_service.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class OrderResponse {

    private Long id;
    private String status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;

}
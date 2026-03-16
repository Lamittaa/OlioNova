package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderResponse {

    private Long id;
    private Long customerId;
    private String status;
    private List<OrderItemResponse> items;
    private boolean isMember;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




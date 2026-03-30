package com.project.order.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDashboardResponse {

    private Long orderId;

    private String status;

    private List<OrderItemStatusResponse> items;
}
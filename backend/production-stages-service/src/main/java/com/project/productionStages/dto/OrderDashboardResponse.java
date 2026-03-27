package com.project.productionStages.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDashboardResponse {

    private Long orderId;

    private int itemsCount;

    private long completedItems;

    private List<OrderItemStatusResponse> items;
}
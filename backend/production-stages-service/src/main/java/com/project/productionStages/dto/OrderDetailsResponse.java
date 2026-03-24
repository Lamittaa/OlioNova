package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDetailsResponse {

    private Long orderId;

    private String status;

    private List<OrderItemStageResponse> items;
}
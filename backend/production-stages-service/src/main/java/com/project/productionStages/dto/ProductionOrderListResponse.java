package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionOrderListResponse {

    private Long orderId;

    private String status;

    private Integer totalItems;

    private Integer completedItems;

    private Integer queueNumber;
}
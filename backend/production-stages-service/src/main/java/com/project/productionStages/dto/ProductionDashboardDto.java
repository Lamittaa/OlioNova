package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionDashboardDto {

    private Long orderId;

    private String status; // IN_QUEUE / IN_PROGRESS / COMPLETED

    private int itemsCount;

    private long completedItems;

    private Integer queueNumber; // فقط إذا waiting\\

    private List<String> storage;
}
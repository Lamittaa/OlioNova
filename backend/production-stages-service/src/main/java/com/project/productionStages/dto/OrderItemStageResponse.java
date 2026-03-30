package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemStageResponse {

    private Long orderItemId;

    private String itemStatus;

    private String oliveType;

    private Double weight;

    private String line;

    private String stageType;

    private Integer stageOrder;

    private String container;
}
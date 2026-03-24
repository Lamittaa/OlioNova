package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemStageResponse {

    private Long orderItemId;

    private String oliveType;

    private Double weight;

    private String line;

    private String stageType;

    private String container;
}
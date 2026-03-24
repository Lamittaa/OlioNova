package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemResponse {

    private Long orderItemId;

    private Long orderId;

    private String oliveType;

    private Double weight;
}
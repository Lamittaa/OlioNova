package com.project.productionStages.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartProductionRequest {
    private String line;
    private String stageType;
    private String container; 
    private Long orderItemId;
    private Long orderId;
}
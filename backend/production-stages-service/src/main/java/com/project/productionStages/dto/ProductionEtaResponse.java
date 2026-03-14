package com.project.productionStages.dto;

import com.project.productionStages.model.StageType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionEtaResponse {

    private Long orderItemId;
    
    private String line;
    private StageType currentStage;
    private long remainingMinutes;
    private int queue;
    private long eta;

}
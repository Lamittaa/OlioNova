package com.project.productionStages.dto;

import com.project.productionStages.model.StageStatus;
import com.project.productionStages.model.StageType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionDashboardResponse {

    private String line;
    private StageType stage;
    private StageStatus status;
    private long remainingMinutes;
    private int queue;
    private long eta;
    private long avgStageTime;
    private int throughputPerHour;

}
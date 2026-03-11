package com.project.productionStages.dto;

import com.project.productionStages.model.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionStageResponse {

    private Long id;

    private String name;

    private StageType stageType;

    private Long orderId;

    private Long orderItemId;

    private String line;

    private Integer stageOrder;

    private StageStatus currentStatus;
}
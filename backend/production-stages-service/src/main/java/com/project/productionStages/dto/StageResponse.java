package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class StageResponse {

    private Long stageId;
    private String stageType;
    private String containerName;
    private String stageStatus;
    private ItemResponse item;

}
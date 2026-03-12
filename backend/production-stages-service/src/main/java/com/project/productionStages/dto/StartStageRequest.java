package com.project.productionStages.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartStageRequest {

    @NotNull(message = "stageId is required")
    private Long stageId;

    @NotNull(message = "userId is required")
    private Long userId;
}
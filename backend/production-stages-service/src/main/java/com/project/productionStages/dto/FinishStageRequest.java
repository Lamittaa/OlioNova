package com.project.productionStages.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinishStageRequest {

    @NotNull(message = "stageId is required")
    private Long stageId;

    @NotNull(message = "employeeId is required")
    private Long employeeId;
}

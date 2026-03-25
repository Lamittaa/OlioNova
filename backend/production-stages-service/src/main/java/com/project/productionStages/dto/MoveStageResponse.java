package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MoveStageResponse {
    private boolean isLastStage;
    private String message;
}
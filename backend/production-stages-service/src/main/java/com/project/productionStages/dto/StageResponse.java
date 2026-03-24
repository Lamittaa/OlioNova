package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class StageResponse {

    private String stageType;
    private String stageLabel;
    private ItemResponse item;
    private List<ContainerResponse> containers;
}
package com.project.productionStages.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LineResponse {

    private String line; 

    private List<StageResponse> stages;
}
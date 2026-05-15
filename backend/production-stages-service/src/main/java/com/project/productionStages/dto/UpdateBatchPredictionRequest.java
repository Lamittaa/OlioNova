package com.project.productionStages.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBatchPredictionRequest {

    private Long imageId;
    private BigDecimal predictedYieldPercent;
    private BigDecimal predictedOilKg;
    private BigDecimal predictionConfidence;
    private String modelVersion;
}

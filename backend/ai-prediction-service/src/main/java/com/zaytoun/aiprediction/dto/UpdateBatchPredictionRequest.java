package com.zaytoun.aiprediction.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class UpdateBatchPredictionRequest {
    Long imageId;
    BigDecimal predictedYieldPercent;
    BigDecimal predictedOilKg;
    BigDecimal predictionConfidence;
    String modelVersion;
}

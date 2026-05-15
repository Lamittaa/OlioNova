package com.zaytoun.aiprediction.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductionBatchResponse {
    private String batchId;
    private Long orderId;
    private Long orderItemId;
    private BigDecimal oliveWeightKg;
    private String status;
    private BigDecimal predictedYieldPercent;
    private BigDecimal predictedOilKg;
    private BigDecimal predictionConfidence;
    private String modelVersion;
    private Long latestImageId;
}

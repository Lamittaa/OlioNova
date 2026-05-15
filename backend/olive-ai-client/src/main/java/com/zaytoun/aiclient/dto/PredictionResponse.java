package com.zaytoun.aiclient.dto;

import lombok.Data;

@Data
public class PredictionResponse {
    private Long imageId;
    private String batchId;
    private Double predictedYieldPercent;
    private Double confidence;
    private String modelVersion;
}

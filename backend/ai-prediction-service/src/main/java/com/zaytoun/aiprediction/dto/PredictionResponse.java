package com.zaytoun.aiprediction.dto;

import com.zaytoun.aiprediction.model.OliveImage;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

@Value
@Builder
public class PredictionResponse {
    Long imageId;
    String batchId;
    Double predictedYieldPercent;
    BigDecimal oliveWeightKg;
    BigDecimal predictedOilKg;
    Double confidence;
    String modelVersion;
    Double rMean;
    Double gMean;
    Double bMean;
    Map<String, Double> featureValues;

    public static PredictionResponse from(OliveImage image, Map<String, Double> featureValues) {
        return PredictionResponse.builder()
                .imageId(image.getId())
                .batchId(image.getBatchId())
                .predictedYieldPercent(image.getPredictedYieldPercent())
                .oliveWeightKg(image.getOliveWeightKg())
                .predictedOilKg(image.getPredictedOilKg())
                .confidence(image.getPredictionConfidence())
                .modelVersion(image.getModelVersion())
                .rMean(image.getRMean())
                .gMean(image.getGMean())
                .bMean(image.getBMean())
                .featureValues(featureValues)
                .build();
    }
}

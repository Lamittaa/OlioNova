package com.zaytoun.aiprediction.dto;

import com.zaytoun.aiprediction.model.OliveImage;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Value
@Builder
public class DatasetImageResponse {
    Long imageId;
    String batchId;
    LocalDateTime captureTime;
    boolean segmentationSuccess;
    Double actualYieldPercent;
    Double predictedYieldPercent;
    Double predictionConfidence;
    String modelVersion;
    Boolean anomalyFlag;
    Map<String, Double> featureValues;

    public static DatasetImageResponse from(OliveImage image, Map<String, Double> featureValues) {
        return DatasetImageResponse.builder()
                .imageId(image.getId())
                .batchId(image.getBatchId())
                .captureTime(image.getCaptureTime())
                .segmentationSuccess(Boolean.TRUE.equals(image.getSegmentationSuccess()))
                .actualYieldPercent(image.getActualYieldPercent())
                .predictedYieldPercent(image.getPredictedYieldPercent())
                .predictionConfidence(image.getPredictionConfidence())
                .modelVersion(image.getModelVersion())
                .anomalyFlag(image.getAnomalyFlag())
                .featureValues(featureValues)
                .build();
    }
}

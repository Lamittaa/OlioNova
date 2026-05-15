package com.project.productionStages.dto;

import com.project.productionStages.model.ProductionBatch;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductionBatchResponse from(ProductionBatch batch) {
        return ProductionBatchResponse.builder()
                .batchId(batch.getBatchId())
                .orderId(batch.getOrderId())
                .orderItemId(batch.getOrderItemId())
                .oliveWeightKg(batch.getOliveWeightKg())
                .status(batch.getStatus())
                .predictedYieldPercent(batch.getPredictedYieldPercent())
                .predictedOilKg(batch.getPredictedOilKg())
                .predictionConfidence(batch.getPredictionConfidence())
                .modelVersion(batch.getModelVersion())
                .latestImageId(batch.getLatestImageId())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}

package com.ops.tracking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public BigDecimal getOliveWeightKg() {
        return oliveWeightKg;
    }

    public void setOliveWeightKg(BigDecimal oliveWeightKg) {
        this.oliveWeightKg = oliveWeightKg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPredictedYieldPercent() {
        return predictedYieldPercent;
    }

    public void setPredictedYieldPercent(BigDecimal predictedYieldPercent) {
        this.predictedYieldPercent = predictedYieldPercent;
    }

    public BigDecimal getPredictedOilKg() {
        return predictedOilKg;
    }

    public void setPredictedOilKg(BigDecimal predictedOilKg) {
        this.predictedOilKg = predictedOilKg;
    }

    public BigDecimal getPredictionConfidence() {
        return predictionConfidence;
    }

    public void setPredictionConfidence(BigDecimal predictionConfidence) {
        this.predictionConfidence = predictionConfidence;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Long getLatestImageId() {
        return latestImageId;
    }

    public void setLatestImageId(Long latestImageId) {
        this.latestImageId = latestImageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

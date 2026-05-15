package com.ops.tracking_service.dto;

import com.ops.tracking_service.model.TankCode;
import com.ops.tracking_service.model.TrackingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TrackingResponse {
    private String batchId;
    private String trackingCode;
    private Long orderId;
    private Long orderItemId;
    private BigDecimal oliveWeightKg;
    private BigDecimal predictedOilKg;
    private TrackingStatus status;
    private String statusLabel;
    private int progressPercent;
    private int estimatedTotalMinutes;
    private int estimatedRemainingMinutes;
    private TankCode tankCode;
    private String tankLabel;
    private String productionLine;
    private String friendlyMessage;
    private List<TankResponse> tanks;
    private LocalDateTime registeredAt;
    private LocalDateTime startedAt;
    private LocalDateTime estimatedDoneAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public TrackingResponse(String batchId, String trackingCode, Long orderId, Long orderItemId, BigDecimal oliveWeightKg,
                            BigDecimal predictedOilKg, TrackingStatus status, String statusLabel,
                            int progressPercent, int estimatedTotalMinutes, int estimatedRemainingMinutes,
                            TankCode tankCode, String tankLabel, String productionLine, String friendlyMessage, List<TankResponse> tanks,
                            LocalDateTime registeredAt, LocalDateTime startedAt, LocalDateTime estimatedDoneAt,
                            LocalDateTime completedAt, LocalDateTime updatedAt) {
        this.batchId = batchId;
        this.trackingCode = trackingCode;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.oliveWeightKg = oliveWeightKg;
        this.predictedOilKg = predictedOilKg;
        this.status = status;
        this.statusLabel = statusLabel;
        this.progressPercent = progressPercent;
        this.estimatedTotalMinutes = estimatedTotalMinutes;
        this.estimatedRemainingMinutes = estimatedRemainingMinutes;
        this.tankCode = tankCode;
        this.tankLabel = tankLabel;
        this.productionLine = productionLine;
        this.friendlyMessage = friendlyMessage;
        this.tanks = tanks;
        this.registeredAt = registeredAt;
        this.startedAt = startedAt;
        this.estimatedDoneAt = estimatedDoneAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public String getBatchId() { return batchId; }
    public String getTrackingCode() { return trackingCode; }
    public Long getOrderId() { return orderId; }
    public Long getOrderItemId() { return orderItemId; }
    public BigDecimal getOliveWeightKg() { return oliveWeightKg; }
    public BigDecimal getPredictedOilKg() { return predictedOilKg; }
    public TrackingStatus getStatus() { return status; }
    public String getStatusLabel() { return statusLabel; }
    public int getProgressPercent() { return progressPercent; }
    public int getEstimatedTotalMinutes() { return estimatedTotalMinutes; }
    public int getEstimatedRemainingMinutes() { return estimatedRemainingMinutes; }
    public TankCode getTankCode() { return tankCode; }
    public String getTankLabel() { return tankLabel; }
    public String getProductionLine() { return productionLine; }
    public String getFriendlyMessage() { return friendlyMessage; }
    public List<TankResponse> getTanks() { return tanks; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEstimatedDoneAt() { return estimatedDoneAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

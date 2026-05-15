package com.ops.tracking_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_tracking")
public class BatchTracking {

    @Id
    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;

    @Column(name = "tracking_code", unique = true, length = 32)
    private String trackingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TrackingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tank_code", nullable = false, length = 1)
    private TankCode tankCode;

    @Column(name = "production_line", length = 10)
    private String productionLine;

    @Column(name = "estimated_total_minutes", nullable = false)
    private Integer estimatedTotalMinutes;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BatchTracking() {
    }

    public BatchTracking(String batchId, TrackingStatus status, TankCode tankCode, Integer estimatedTotalMinutes,
                         LocalDateTime registeredAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.batchId = batchId;
        this.status = status;
        this.tankCode = tankCode;
        this.estimatedTotalMinutes = estimatedTotalMinutes;
        this.registeredAt = registeredAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public TrackingStatus getStatus() { return status; }
    public void setStatus(TrackingStatus status) { this.status = status; }
    public TankCode getTankCode() { return tankCode; }
    public void setTankCode(TankCode tankCode) { this.tankCode = tankCode; }
    public String getProductionLine() { return productionLine; }
    public void setProductionLine(String productionLine) { this.productionLine = productionLine; }
    public Integer getEstimatedTotalMinutes() { return estimatedTotalMinutes; }
    public void setEstimatedTotalMinutes(Integer estimatedTotalMinutes) { this.estimatedTotalMinutes = estimatedTotalMinutes; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (registeredAt == null) {
            registeredAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = TrackingStatus.REGISTERED;
        }
        if (estimatedTotalMinutes == null) {
            estimatedTotalMinutes = 45;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

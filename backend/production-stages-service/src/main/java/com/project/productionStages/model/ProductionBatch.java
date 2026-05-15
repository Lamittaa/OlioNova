package com.project.productionStages.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionBatch {

    @Id
    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "olive_weight_kg", precision = 12, scale = 2, nullable = false)
    private BigDecimal oliveWeightKg;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "predicted_yield_percent", precision = 6, scale = 2)
    private BigDecimal predictedYieldPercent;

    @Column(name = "predicted_oil_kg", precision = 12, scale = 2)
    private BigDecimal predictedOilKg;

    @Column(name = "prediction_confidence", precision = 7, scale = 4)
    private BigDecimal predictionConfidence;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "latest_image_id")
    private Long latestImageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "READY_FOR_PREDICTION";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

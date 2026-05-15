package com.zaytoun.aiprediction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "olive_images", indexes = {
        @Index(name = "idx_olive_images_batch_id", columnList = "batch_id"),
        @Index(name = "idx_olive_images_training", columnList = "is_training_data"),
        @Index(name = "idx_olive_images_capture_time", columnList = "capture_time")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OliveImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "capture_time")
    private LocalDateTime captureTime;

    @Column(name = "cultivar")
    private String cultivar;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "r_mean")
    private Double rMean;

    @Column(name = "g_mean")
    private Double gMean;

    @Column(name = "b_mean")
    private Double bMean;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "color_indexes", columnDefinition = "jsonb")
    private String colorIndexes;

    @Column(name = "segmentation_success")
    private Boolean segmentationSuccess;

    @Column(name = "actual_yield_percent")
    private Double actualYieldPercent;

    @Column(name = "yield_recorded_at")
    private LocalDateTime yieldRecordedAt;

    @Column(name = "predicted_yield_percent")
    private Double predictedYieldPercent;

    @Column(name = "olive_weight_kg", precision = 10, scale = 2)
    private BigDecimal oliveWeightKg;

    @Column(name = "predicted_oil_kg", precision = 10, scale = 2)
    private BigDecimal predictedOilKg;

    @Column(name = "prediction_confidence")
    private Double predictionConfidence;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "is_training_data")
    private Boolean isTrainingData;

    @Column(name = "anomaly_flag")
    private Boolean anomalyFlag;
}

package com.zaytoun.aiprediction.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String version;

    @Column(name = "model_type")
    private String modelType;

    @Column(name = "training_date")
    private LocalDateTime trainingDate;

    @Column(name = "training_samples")
    private Integer trainingSamples;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "performance_metrics", columnDefinition = "jsonb")
    private String performanceMetrics;

    @Column(name = "model_path")
    private String modelPath;

    @Column(name = "active")
    private Boolean active;
}

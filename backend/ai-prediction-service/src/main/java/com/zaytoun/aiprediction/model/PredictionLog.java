package com.zaytoun.aiprediction.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private OliveImage oliveImage;

    @Column(name = "predicted_yield")
    private Double predictedYield;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "prediction_time")
    private LocalDateTime predictionTime;

    @Column(name = "latency_ms")
    private Integer latencyMs;
}

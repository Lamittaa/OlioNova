package com.zaytoun.aiprediction.ml;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TrainingJobStatus {
    String jobId;
    TrainingJobState state;
    LocalDateTime createdAt;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    TrainingMetrics metrics;
    String errorMessage;
}

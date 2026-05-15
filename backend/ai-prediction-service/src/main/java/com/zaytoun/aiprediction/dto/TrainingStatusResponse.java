package com.zaytoun.aiprediction.dto;

import com.zaytoun.aiprediction.ml.TrainingJobStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TrainingStatusResponse {
    String jobId;
    String state;
    LocalDateTime createdAt;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    Object metrics;
    String errorMessage;

    public static TrainingStatusResponse from(TrainingJobStatus status) {
        return TrainingStatusResponse.builder()
                .jobId(status.getJobId())
                .state(status.getState().name())
                .createdAt(status.getCreatedAt())
                .startedAt(status.getStartedAt())
                .completedAt(status.getCompletedAt())
                .metrics(status.getMetrics())
                .errorMessage(status.getErrorMessage())
                .build();
    }
}

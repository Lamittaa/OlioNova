package com.zaytoun.aiprediction.dto;

import com.zaytoun.aiprediction.ml.TrainingJobStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrainingJobResponse {
    String jobId;
    String state;
    String statusUrl;

    public static TrainingJobResponse from(TrainingJobStatus status) {
        return TrainingJobResponse.builder()
                .jobId(status.getJobId())
                .state(status.getState().name())
                .statusUrl("/api/v1/train/" + status.getJobId())
                .build();
    }
}

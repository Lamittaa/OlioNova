package com.zaytoun.aiprediction.ml;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrainingMetrics {
    String modelType;
    String modelVersion;
    int trainingSamples;
    double r2;
    double rmse;
    double mae;
    boolean activated;
    String notes;
}

package com.zaytoun.aiprediction.ml;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PredictionResult {
    double predictedYield;
    double confidence;
    String modelVersion;
}

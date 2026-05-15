package com.zaytoun.aiprediction.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelArtifact {
    private String modelVersion;
    private String modelType;
    private LocalDateTime trainingDate;
    private int trainingSamples;
    private double[] weights;
    private double intercept;
    private double r2;
    private double rmse;
    private double mae;
    private double residualStdDev;
    private Map<String, Double> featureMeans;
    private Map<String, Double> featureStdDevs;
    private boolean active;
}

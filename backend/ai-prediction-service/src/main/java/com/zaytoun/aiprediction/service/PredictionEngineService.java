package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.exception.ColdStartException;
import com.zaytoun.aiprediction.ml.FeatureVector;
import com.zaytoun.aiprediction.ml.ModelArtifact;
import com.zaytoun.aiprediction.ml.PredictionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PredictionEngineService {
    private final ModelRegistryService modelRegistryService;
    private final LinearRegressionTrainerService linearRegressionTrainerService;

    @Value("${ai.prediction.minimum-training-samples:8}")
    private int minimumTrainingSamples;

    public PredictionResult predict(FeatureVector featureVector) {
        ModelArtifact artifact = modelRegistryService.getActiveModel()
                .orElseThrow(() -> new ColdStartException("No active model found. Collect labeled samples and train a model first."));

        if (artifact.getTrainingSamples() < minimumTrainingSamples) {
            throw new ColdStartException("Not enough labeled samples. Minimum required: " + minimumTrainingSamples);
        }

        double predictedYield = linearRegressionTrainerService.predict(artifact, featureVector);
        predictedYield = Math.max(0.0, Math.min(100.0, predictedYield));

        double distanceScore = featureVector.toPersistedMap().entrySet().stream()
                .mapToDouble(entry -> {
                    double mean = artifact.getFeatureMeans().getOrDefault(entry.getKey(), 0.0);
                    double std = Math.max(artifact.getFeatureStdDevs().getOrDefault(entry.getKey(), 1.0), 1e-6);
                    return Math.abs(entry.getValue() - mean) / std;
                })
                .average()
                .orElse(0.0);
        double confidence = Math.max(0.05, Math.min(0.99, artifact.getR2() * Math.exp(-0.12 * distanceScore)));

        return PredictionResult.builder()
                .predictedYield(predictedYield)
                .confidence(confidence)
                .modelVersion(artifact.getModelVersion())
                .build();
    }
}

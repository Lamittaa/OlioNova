package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.dto.TrainingRequest;
import com.zaytoun.aiprediction.exception.ResourceNotFoundException;
import com.zaytoun.aiprediction.ml.*;
import com.zaytoun.aiprediction.model.OliveImage;
import com.zaytoun.aiprediction.repository.OliveImageRepository;
import com.zaytoun.aiprediction.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingJobService {
    private final OliveImageRepository oliveImageRepository;
    private final JsonUtils jsonUtils;
    private final LinearRegressionTrainerService linearRegressionTrainerService;
    private final ModelRegistryService modelRegistryService;
    @Qualifier("trainingExecutor")
    private final java.util.concurrent.Executor trainingExecutor;
    private final Map<String, TrainingJobStatus> jobStatuses = new ConcurrentHashMap<>();

    @Value("${ai.prediction.minimum-training-samples:8}")
    private int minimumTrainingSamples;

    public TrainingJobStatus queueTraining(TrainingRequest request) {
        String jobId = UUID.randomUUID().toString();
        TrainingJobStatus status = TrainingJobStatus.builder()
                .jobId(jobId)
                .state(TrainingJobState.QUEUED)
                .createdAt(LocalDateTime.now())
                .build();
        jobStatuses.put(jobId, status);
        trainingExecutor.execute(() -> runTraining(jobId, request));
        return status;
    }

    public TrainingJobStatus getStatus(String jobId) {
        TrainingJobStatus status = jobStatuses.get(jobId);
        if (status == null) {
            throw new ResourceNotFoundException("Training job not found: " + jobId);
        }
        return status;
    }

    public void runTraining(String jobId, TrainingRequest request) {
        jobStatuses.computeIfPresent(jobId, (k, old) -> TrainingJobStatus.builder()
                .jobId(jobId)
                .state(TrainingJobState.RUNNING)
                .createdAt(old.getCreatedAt())
                .startedAt(LocalDateTime.now())
                .build());
        try {
            List<OliveImage> trainingImages = oliveImageRepository.findAllByActualYieldPercentIsNotNullOrderByCaptureTimeAsc();
            if (trainingImages.size() < minimumTrainingSamples) {
                throw new IllegalStateException("Not enough labeled samples to train a model. Current count: " + trainingImages.size());
            }

            List<FeatureVector> vectors = new ArrayList<>();
            List<Double> labels = new ArrayList<>();
            for (OliveImage image : trainingImages) {
                Map<String, Double> map = jsonUtils.readDoubleMap(image.getColorIndexes());
                double r = map.getOrDefault("R", image.getRMean());
                double g = map.getOrDefault("G", image.getGMean());
                double b = map.getOrDefault("B", image.getBMean());
                Map<String, Double> features = new LinkedHashMap<>(map);
                features.remove("R");
                features.remove("G");
                features.remove("B");
                vectors.add(FeatureVector.builder().rMean(r).gMean(g).bMean(b).features(features).segmentationSuccess(Boolean.TRUE.equals(image.getSegmentationSuccess())).build());
                labels.add(image.getActualYieldPercent());
            }

            String normalizedType = normalizeModelType(request.getModelType());
            String notes = normalizedType.equals("linear")
                    ? "Trained linear regression model."
                    : "Requested model type '" + request.getModelType() + "' is mapped to linear regression in this implementation for deterministic JVM-only deployment.";
            String version = normalizedType + "-" + System.currentTimeMillis();
            ModelArtifact artifact = linearRegressionTrainerService.train(vectors, labels, normalizedType, version);
            double currentR2 = modelRegistryService.getActiveModel().map(ModelArtifact::getR2).orElse(Double.NEGATIVE_INFINITY);
            boolean activate = request.isForceActivate() || artifact.getR2() >= currentR2;
            modelRegistryService.save(artifact, activate);

            TrainingMetrics metrics = TrainingMetrics.builder()
                    .modelType(normalizedType)
                    .modelVersion(version)
                    .trainingSamples(artifact.getTrainingSamples())
                    .r2(artifact.getR2())
                    .rmse(artifact.getRmse())
                    .mae(artifact.getMae())
                    .activated(activate)
                    .notes(notes)
                    .build();

            jobStatuses.put(jobId, TrainingJobStatus.builder()
                    .jobId(jobId)
                    .state(TrainingJobState.COMPLETED)
                    .createdAt(jobStatuses.get(jobId).getCreatedAt())
                    .startedAt(jobStatuses.get(jobId).getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .metrics(metrics)
                    .build());
        } catch (Exception e) {
            log.error("Training job {} failed", jobId, e);
            TrainingJobStatus current = jobStatuses.get(jobId);
            jobStatuses.put(jobId, TrainingJobStatus.builder()
                    .jobId(jobId)
                    .state(TrainingJobState.FAILED)
                    .createdAt(current.getCreatedAt())
                    .startedAt(current.getStartedAt())
                    .completedAt(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    private String normalizeModelType(String requested) {
        if (requested == null || requested.isBlank()) {
            return "linear";
        }
        String lower = requested.trim().toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "linear", "xgboost", "bpnn" -> lower;
            default -> "linear";
        };
    }
}

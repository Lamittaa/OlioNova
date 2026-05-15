package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.dto.DatasetImageResponse;
import com.zaytoun.aiprediction.exception.ResourceNotFoundException;
import com.zaytoun.aiprediction.ml.FeatureVector;
import com.zaytoun.aiprediction.ml.PredictionResult;
import com.zaytoun.aiprediction.model.OliveImage;
import com.zaytoun.aiprediction.repository.OliveImageRepository;
import com.zaytoun.aiprediction.util.JsonUtils;
import com.zaytoun.aiprediction.validation.BatchIdRules;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatasetService {
    private final StorageService storageService;
    private final FeatureExtractionService featureExtractionService;
    private final OliveImageRepository oliveImageRepository;
    private final JsonUtils jsonUtils;
    private final PredictionEngineService predictionEngineService;

    @Value("${ai.anomaly.absolute-error-threshold:5.0}")
    private double anomalyThreshold;

    public DatasetImageResponse storeDatasetImage(MultipartFile file, String batchId, String cultivar, LocalDate harvestDate, Double actualYieldPercent) {
        String normalizedBatchId = BatchIdRules.requireValid(batchId);
        Path path = storageService.storeImage(file, normalizedBatchId);
        FeatureVector featureVector = featureExtractionService.extract(path);

        OliveImage entity = OliveImage.builder()
                .batchId(normalizedBatchId)
                .imagePath(path.toString())
                .captureTime(LocalDateTime.now())
                .cultivar(cultivar)
                .harvestDate(harvestDate)
                .rMean(featureVector.getRMean())
                .gMean(featureVector.getGMean())
                .bMean(featureVector.getBMean())
                .colorIndexes(jsonUtils.toJson(featureVector.toPersistedMap()))
                .segmentationSuccess(featureVector.isSegmentationSuccess())
                .actualYieldPercent(actualYieldPercent)
                .yieldRecordedAt(actualYieldPercent == null ? null : LocalDateTime.now())
                .isTrainingData(actualYieldPercent != null)
                .anomalyFlag(false)
                .build();

        try {
            PredictionResult prediction = predictionEngineService.predict(featureVector);
            entity.setPredictedYieldPercent(prediction.getPredictedYield());
            entity.setPredictionConfidence(prediction.getConfidence());
            entity.setModelVersion(prediction.getModelVersion());
            if (actualYieldPercent != null) {
                entity.setAnomalyFlag(Math.abs(actualYieldPercent - prediction.getPredictedYield()) > anomalyThreshold);
            }
        } catch (Exception ignored) {
            // Cold start is acceptable for dataset ingestion.
        }

        OliveImage saved = oliveImageRepository.save(entity);
        return DatasetImageResponse.from(saved, jsonUtils.readDoubleMap(saved.getColorIndexes()));
    }

    public DatasetImageResponse updateBatchYield(String batchId, Double actualYieldPercent) {
        String normalizedBatchId = BatchIdRules.requireValid(batchId);
        List<OliveImage> images = oliveImageRepository.findAllByBatchIdOrderByCaptureTimeDesc(normalizedBatchId);
        if (images.isEmpty()) {
            throw new ResourceNotFoundException("No images found for batch " + normalizedBatchId);
        }
        OliveImage latest = images.getFirst();
        latest.setActualYieldPercent(actualYieldPercent);
        latest.setYieldRecordedAt(LocalDateTime.now());
        latest.setIsTrainingData(true);
        if (latest.getPredictedYieldPercent() != null) {
            latest.setAnomalyFlag(Math.abs(actualYieldPercent - latest.getPredictedYieldPercent()) > anomalyThreshold);
        }
        OliveImage saved = oliveImageRepository.save(latest);
        return DatasetImageResponse.from(saved, jsonUtils.readDoubleMap(saved.getColorIndexes()));
    }
}

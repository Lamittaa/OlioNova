package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.client.ProductionBatchClient;
import com.zaytoun.aiprediction.dto.ProductionBatchResponse;
import com.zaytoun.aiprediction.dto.PredictionResponse;
import com.zaytoun.aiprediction.dto.UpdateBatchPredictionRequest;
import com.zaytoun.aiprediction.exception.ResourceNotFoundException;
import com.zaytoun.aiprediction.ml.FeatureVector;
import com.zaytoun.aiprediction.ml.PredictionResult;
import com.zaytoun.aiprediction.model.OliveImage;
import com.zaytoun.aiprediction.model.PredictionLog;
import com.zaytoun.aiprediction.repository.OliveImageRepository;
import com.zaytoun.aiprediction.repository.PredictionLogRepository;
import com.zaytoun.aiprediction.util.JsonUtils;
import com.zaytoun.aiprediction.validation.BatchIdRules;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PredictionService {
    private final StorageService storageService;
    private final FeatureExtractionService featureExtractionService;
    private final PredictionEngineService predictionEngineService;
    private final OliveImageRepository oliveImageRepository;
    private final PredictionLogRepository predictionLogRepository;
    private final ProductionBatchClient productionBatchClient;
    private final JsonUtils jsonUtils;

    public PredictionResponse predict(MultipartFile file, String batchId, String cultivar, LocalDate harvestDate) {
        String normalizedBatchId = BatchIdRules.requireValid(batchId);
        ProductionBatchResponse batch = fetchBatch(normalizedBatchId);
        return predictForBatch(file, batch, cultivar, harvestDate);
    }

    public PredictionResponse predictByOrderId(MultipartFile file, Long orderId, String cultivar, LocalDate harvestDate) {
        ProductionBatchResponse batch = fetchBatchByOrderId(orderId);
        return predictForBatch(file, batch, cultivar, harvestDate);
    }

    private PredictionResponse predictForBatch(MultipartFile file, ProductionBatchResponse batch, String cultivar, LocalDate harvestDate) {
        long start = System.currentTimeMillis();
        String normalizedBatchId = BatchIdRules.requireValid(batch.getBatchId());
        BigDecimal oliveWeightKg = requirePositiveOliveWeight(batch);
        Path path = storageService.storeImage(file, normalizedBatchId);
        FeatureVector featureVector = featureExtractionService.extract(path);
        PredictionResult result = predictionEngineService.predict(featureVector);
        BigDecimal predictedOilKg = calculatePredictedOilKg(oliveWeightKg, result.getPredictedYield());

        OliveImage oliveImage = OliveImage.builder()
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
                .predictedYieldPercent(result.getPredictedYield())
                .oliveWeightKg(oliveWeightKg)
                .predictedOilKg(predictedOilKg)
                .predictionConfidence(result.getConfidence())
                .modelVersion(result.getModelVersion())
                .isTrainingData(false)
                .anomalyFlag(false)
                .build();

        OliveImage savedImage = oliveImageRepository.save(oliveImage);
        predictionLogRepository.save(PredictionLog.builder()
                .oliveImage(savedImage)
                .predictedYield(result.getPredictedYield())
                .confidence(result.getConfidence())
                .modelVersion(result.getModelVersion())
                .predictionTime(LocalDateTime.now())
                .latencyMs((int) (System.currentTimeMillis() - start))
                .build());

        try {
            productionBatchClient.updatePrediction(normalizedBatchId, UpdateBatchPredictionRequest.builder()
                    .imageId(savedImage.getId())
                    .predictedYieldPercent(BigDecimal.valueOf(result.getPredictedYield()).setScale(2, RoundingMode.HALF_UP))
                    .predictedOilKg(predictedOilKg)
                    .predictionConfidence(BigDecimal.valueOf(result.getConfidence()).setScale(4, RoundingMode.HALF_UP))
                    .modelVersion(result.getModelVersion())
                    .build());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Production batch not found with id: " + normalizedBatchId);
        } catch (FeignException ex) {
            throw new IllegalStateException(buildProductionBatchError("update prediction", ex));
        }

        return PredictionResponse.from(savedImage, jsonUtils.readDoubleMap(savedImage.getColorIndexes()));
    }

    private ProductionBatchResponse fetchBatch(String batchId) {
        try {
            return productionBatchClient.getBatch(batchId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Production batch not found with id: " + batchId);
        } catch (FeignException ex) {
            throw new IllegalStateException(buildProductionBatchError("load batch", ex));
        }
    }

    private ProductionBatchResponse fetchBatchByOrderId(Long orderId) {
        try {
            return productionBatchClient.createOrGetBatchByOrderId(orderId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        } catch (FeignException ex) {
            throw new IllegalStateException(buildProductionBatchError("create or load batch by order", ex));
        }
    }

    private String buildProductionBatchError(String action, FeignException ex) {
        String detail = ex.contentUTF8();
        if (detail == null || detail.isBlank()) {
            detail = ex.getMessage();
        }
        return "Production batch service failed while trying to " + action + " (status " + ex.status() + "): " + detail;
    }

    private BigDecimal requirePositiveOliveWeight(ProductionBatchResponse batch) {
        if (batch.getOliveWeightKg() == null || batch.getOliveWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Production batch must include a positive olive weight before prediction");
        }
        return batch.getOliveWeightKg();
    }

    private BigDecimal calculatePredictedOilKg(BigDecimal oliveWeightKg, Double predictedYieldPercent) {
        return oliveWeightKg
                .multiply(BigDecimal.valueOf(predictedYieldPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

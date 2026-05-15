package com.zaytoun.aiprediction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaytoun.aiprediction.ml.ModelArtifact;
import com.zaytoun.aiprediction.model.ModelVersion;
import com.zaytoun.aiprediction.repository.ModelVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelRegistryService {
    private final ModelVersionRepository modelVersionRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final com.zaytoun.aiprediction.util.JsonUtils jsonUtils;
    private final AtomicReference<ModelArtifact> activeModel = new AtomicReference<>();

    public Optional<ModelArtifact> getActiveModel() {
        ModelArtifact cached = activeModel.get();
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ModelVersion> activeVersion = modelVersionRepository.findFirstByActiveTrueOrderByTrainingDateDesc();
        if (activeVersion.isPresent()) {
            return Optional.of(loadArtifact(activeVersion.get()));
        }

        Optional<ModelVersion> latestVersion = modelVersionRepository.findFirstByOrderByTrainingDateDesc();
        if (latestVersion.isPresent()) {
            return Optional.of(promoteExistingVersion(latestVersion.get()));
        }

        return loadLatestModelFromDisk();
    }

    public ModelArtifact save(ModelArtifact artifact, boolean activate) {
        try {
            Path path = storageService.modelPath(artifact.getModelVersion());
            Files.writeString(path, objectMapper.writeValueAsString(artifact));
            if (activate) {
                modelVersionRepository.findFirstByActiveTrueOrderByTrainingDateDesc().ifPresent(existing -> {
                    existing.setActive(false);
                    modelVersionRepository.save(existing);
                });
            }
            ModelVersion mv = ModelVersion.builder()
                    .version(artifact.getModelVersion())
                    .modelType(artifact.getModelType())
                    .trainingDate(Optional.ofNullable(artifact.getTrainingDate()).orElse(LocalDateTime.now()))
                    .trainingSamples(artifact.getTrainingSamples())
                    .performanceMetrics(objectMapper.writeValueAsString(Map.of(
                            "r2", artifact.getR2(),
                            "rmse", artifact.getRmse(),
                            "mae", artifact.getMae(),
                            "residualStdDev", artifact.getResidualStdDev())))
                    .modelPath(path.toString())
                    .active(activate)
                    .build();
            modelVersionRepository.save(mv);
            if (activate) {
                activeModel.set(artifact);
            }
            return artifact;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist model artifact", e);
        }
    }

    private ModelArtifact loadArtifact(ModelVersion version) {
        try {
            ModelArtifact artifact = objectMapper.readValue(Files.readString(Path.of(version.getModelPath())), ModelArtifact.class);
            if (Boolean.TRUE.equals(version.getActive())) {
                activeModel.compareAndSet(null, artifact);
            }
            return artifact;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load active model", e);
        }
    }

    private ModelArtifact promoteExistingVersion(ModelVersion version) {
        ModelArtifact artifact = loadArtifact(version);
        activateVersion(version);
        activeModel.set(artifact);
        return artifact;
    }

    private Optional<ModelArtifact> loadLatestModelFromDisk() {
        try (Stream<Path> files = Files.list(storageService.modelsDirectory())) {
            Optional<Path> latestModel = files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

            if (latestModel.isEmpty()) {
                return Optional.empty();
            }

            Path path = latestModel.get();
            ModelArtifact artifact = objectMapper.readValue(Files.readString(path), ModelArtifact.class);

            ModelVersion version = modelVersionRepository.findByVersion(artifact.getModelVersion())
                    .orElseGet(() -> ModelVersion.builder()
                            .version(artifact.getModelVersion())
                            .modelType(artifact.getModelType())
                            .trainingDate(Optional.ofNullable(artifact.getTrainingDate()).orElse(LocalDateTime.now()))
                            .trainingSamples(artifact.getTrainingSamples())
                            .performanceMetrics(toMetricsJson(artifact))
                            .modelPath(path.toString())
                            .active(false)
                            .build());

            activateVersion(version);
            activeModel.set(artifact);
            log.info("Loaded model {} from disk as the active model fallback.", artifact.getModelVersion());
            return Optional.of(artifact);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load latest model from disk", e);
        }
    }

    private String toMetricsJson(ModelArtifact artifact) {
        return jsonUtils.toJson(Map.of(
                "r2", artifact.getR2(),
                "rmse", artifact.getRmse(),
                "mae", artifact.getMae(),
                "residualStdDev", artifact.getResidualStdDev()));
    }

    private void activateVersion(ModelVersion version) {
        modelVersionRepository.findFirstByActiveTrueOrderByTrainingDateDesc().ifPresent(existing -> {
            existing.setActive(false);
            modelVersionRepository.save(existing);
        });

        version.setActive(true);
        if (version.getTrainingDate() == null) {
            version.setTrainingDate(LocalDateTime.now());
        }
        modelVersionRepository.save(version);
    }
}

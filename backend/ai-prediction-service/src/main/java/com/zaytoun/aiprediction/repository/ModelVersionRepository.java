package com.zaytoun.aiprediction.repository;

import com.zaytoun.aiprediction.model.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {
    Optional<ModelVersion> findFirstByActiveTrueOrderByTrainingDateDesc();
    Optional<ModelVersion> findFirstByOrderByTrainingDateDesc();
    Optional<ModelVersion> findByVersion(String version);
}

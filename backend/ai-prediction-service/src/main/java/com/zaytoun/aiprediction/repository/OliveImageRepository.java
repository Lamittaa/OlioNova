package com.zaytoun.aiprediction.repository;

import com.zaytoun.aiprediction.model.OliveImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OliveImageRepository extends JpaRepository<OliveImage, Long> {
    List<OliveImage> findAllByActualYieldPercentIsNotNullOrderByCaptureTimeAsc();
    List<OliveImage> findAllByBatchIdOrderByCaptureTimeDesc(String batchId);
    Optional<OliveImage> findFirstByBatchIdOrderByCaptureTimeDesc(String batchId);
    long countByActualYieldPercentIsNotNull();
}

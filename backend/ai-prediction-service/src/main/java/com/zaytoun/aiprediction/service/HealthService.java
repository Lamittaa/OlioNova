package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.dto.HealthResponse;
import com.zaytoun.aiprediction.repository.ModelVersionRepository;
import com.zaytoun.aiprediction.repository.OliveImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final OliveImageRepository oliveImageRepository;
    private final ModelVersionRepository modelVersionRepository;

    public HealthResponse getHealth() {
        return HealthResponse.builder()
                .status("UP")
                .activeModelVersion(modelVersionRepository.findFirstByActiveTrueOrderByTrainingDateDesc().map(m -> m.getVersion()).orElse(null))
                .trainingSampleCount(oliveImageRepository.countByActualYieldPercentIsNotNull())
                .storedImageCount(oliveImageRepository.count())
                .build();
    }
}

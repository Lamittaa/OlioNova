package com.project.productionStages.service;

import com.project.productionStages.model.StageType;
import com.project.productionStages.repository.ProductionStageLoggingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TimeEstimationService {

    private final ProductionStageLoggingRepository loggingRepository;

    // ===============================
    // Average Stage Time
    // ===============================
    public long getAverageStageMinutes(StageType stageType) {

        // ✅ نمرر stageType.name() لأن الـ query صارت nativeQuery
        // وتحتاج String مش Enum
        Double avg = loggingRepository.getAverageStageMinutes(stageType.name());

        if (avg == null) {
            return 10; // fallback
        }

        return Math.round(avg);
    }

    // ===============================
    // Remaining Time
    // ===============================
    public long getRemainingMinutes(StageType stageType, LocalDateTime startTime) {

        long avg = getAverageStageMinutes(stageType);

        long elapsed = Duration.between(
                startTime,
                LocalDateTime.now()
        ).toMinutes();

        long remaining = avg - elapsed;

        return Math.max(remaining, 0);
    }

    // ===============================
    // ETA
    // ===============================
    public long estimateQueueTime(
            StageType stageType,
            LocalDateTime startTime,
            int queueCount
    ) {
        long remaining = getRemainingMinutes(stageType, startTime);
        long avg = getAverageStageMinutes(stageType);
        return remaining + (avg * queueCount);
    }
}
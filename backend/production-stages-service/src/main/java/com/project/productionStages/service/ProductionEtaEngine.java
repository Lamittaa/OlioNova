package com.project.productionStages.service;

import com.project.productionStages.model.StageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductionEtaEngine {

    private final TimeEstimationService timeService;
    private final QueueAlgorithmService queueAlgo;

    // =========================================================
    // ETA لطلب ينتظر في الـ Queue
    // =========================================================
    public long calculateEta(
            StageType stageType,
            LocalDateTime stageStartTime,
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ) {

        long remainingTime =
                timeService.getRemainingMinutes(stageType, stageStartTime);

        long averageStageTime =
                timeService.getAverageStageMinutes(stageType);

        int previousGroups =
                queueAlgo.getPreviousGroups(queueNumber, firstWaitingQueue, lineCount);

        return remainingTime + (averageStageTime * previousGroups);
    }

    // =========================================================
    // حساب وقت انتهاء الطلب في الـ Queue
    // =========================================================
    public long calculateFinishTime(
            StageType stageType,
            LocalDateTime stageStartTime,
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ) {

        long remainingTime =
                timeService.getRemainingMinutes(stageType, stageStartTime);

        long averageStageTime =
                timeService.getAverageStageMinutes(stageType);

        int previousGroups =
                queueAlgo.getPreviousGroups(queueNumber, firstWaitingQueue, lineCount);

        return remainingTime + (averageStageTime * (previousGroups + 1));
    }
}
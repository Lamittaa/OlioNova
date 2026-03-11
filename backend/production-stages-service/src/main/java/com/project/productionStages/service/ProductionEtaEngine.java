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
    // حساب ETA لطلب ينتظر (NOT_YET) في الـ Queue
    //
    // الفورمولا من الصور:
    // Estimated time = inProgress_finish_time + avg * previousGroupCount
    //
    // previousGroupCount = int((actualQueue - firstWaitingQueue) / lineCount)
    // =========================================================
    public long calculateEta(
            StageType stageType,
            LocalDateTime stageStartTime,
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ) {
        // وقت انتهاء المرحلة الحالية
        long inProgressFinishTime =
                timeService.getRemainingMinutes(stageType, stageStartTime);

        // متوسط وقت المرحلة
        long avg = timeService.getAverageStageMinutes(stageType);

        // عدد المجموعات السابقة
        int previousGroups = queueAlgo.getPreviousGroups(
                queueNumber,
                firstWaitingQueue,
                lineCount
        );

        // ETA = remaining + avg * previousGroups
        return inProgressFinishTime + (avg * previousGroups);
    }

    // =========================================================
    // حساب وقت انتهاء Queue (للطلب الذي يريد أن يدخل)
    //
    // الفورمولا:
    // finish time = inProgress_finish_time + avg * (previousGroupCount + 1)
    // =========================================================
    public long calculateFinishTime(
            StageType stageType,
            LocalDateTime stageStartTime,
            int queueNumber,
            int firstWaitingQueue,
            int lineCount
    ) {
        long inProgressFinishTime =
                timeService.getRemainingMinutes(stageType, stageStartTime);

        long avg = timeService.getAverageStageMinutes(stageType);

        int previousGroups = queueAlgo.getPreviousGroups(
                queueNumber,
                firstWaitingQueue,
                lineCount
        );

        // finish time = remaining + avg * (previousGroups + 1)
        return inProgressFinishTime + (avg * (previousGroups + 1));
    }
}
package com.project.productionStages.service;

import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.ProductionEtaResponse;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductionEtaService {

    private final ProductionStageRepository stageRepository;
    private final ProductionStageLoggingRepository loggingRepository;
    private final TimeEstimationService timeService;
    private final QueueClient queueClient;
    private final QueueAlgorithmService queueAlgo;
    private final ProductionEtaEngine etaEngine;

    public ProductionEtaResponse getEta(Long orderItemId) {

        // 1 جيب المرحلة الجارية للطلب
        List<ProductionStage> stages =
                stageRepository.findByOrderItemId(orderItemId);

        ProductionStage current = stages.stream()
                .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        if (current == null) return null;

        // 2 جيب الـ log الجاري
        Optional<ProductionStageLogging> logOpt =
                loggingRepository.findByLineAndStageOrderAndEndTimeIsNull(
                        current.getLine(),
                        current.getStageOrder()
                );

        if (logOpt.isEmpty()) return null;

        ProductionStageLogging log = logOpt.get();

        // 3 جيب queue number للطلب
        Integer myQueueNumber = queueClient.getQueueNumber(current.getOrderId());
        if (myQueueNumber == null) return null;

        // 4 جيب أول queue number ينتظر في الخط
        Integer firstWaitingQueueNumber = getFirstWaitingQueueNumber(current.getLine());
        if (firstWaitingQueueNumber == null) {
            firstWaitingQueueNumber = myQueueNumber;
        }

        int lineCount = 2;

        long eta = etaEngine.calculateEta(
                current.getStageType(),
                log.getStartTime(),
                myQueueNumber,
                firstWaitingQueueNumber,
                lineCount
        );

        long remaining = timeService.getRemainingMinutes(
                current.getStageType(),
                log.getStartTime()
        );

        int previousGroups = queueAlgo.getPreviousGroups(
                myQueueNumber,
                firstWaitingQueueNumber,
                lineCount
        );

        return ProductionEtaResponse.builder()
                .orderItemId(orderItemId)
                .line(current.getLine())
                .currentStage(current.getStageType())
                .remainingMinutes(remaining)
                .queue(previousGroups)
                .eta(eta)
                .build();
    }

    // =========================================================
    // HELPER — أول queue ينتظر في الخط
    // ✅ isTemplate=false — order stages فقط
    // =========================================================
    private Integer getFirstWaitingQueueNumber(String line) {

        List<ProductionStage> waitingWashings =
                stageRepository.findByLineAndStageTypeAndCurrentStatusAndIsTemplate(
                        line,
                        StageType.WASHING,
                        StageStatus.NOT_YET,
                        false // order stages فقط
                );

        return waitingWashings.stream()
                .map(s -> queueClient.getQueueNumber(s.getOrderId()))
                .filter(q -> q != null)
                .min(Integer::compareTo)
                .orElse(null);
    }
}
package com.project.productionStages.service;

import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.ProductionEtaResponse;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.*;
import com.project.productionStages.exception.ServiceUnavailableException;

import feign.FeignException;

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

        List<ProductionStage> stages =
                stageRepository.findByOrderItemId(orderItemId);

        ProductionStage current = stages.stream()
                .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        if (current == null) return null;

        Optional<ProductionStageLogging> logOpt =
                loggingRepository.findByLineAndStageOrderAndEndTimeIsNull(
                        current.getLine(),
                        current.getStageOrder()
                );

        if (logOpt.isEmpty()) return null;

        ProductionStageLogging log = logOpt.get();

        Integer myQueueNumber;

        try {

            myQueueNumber =
                    queueClient.getQueueNumber(current.getOrderId());

        } catch (FeignException ex) {

            throw new ServiceUnavailableException(
                    "Queue service is currently unavailable",
                    "QUEUE_SERVICE_DOWN"
            );
        }

        if (myQueueNumber == null) return null;

  
        Integer firstWaitingQueueNumber =
                getFirstWaitingQueueNumber(current.getLine());

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

    
    private Integer getFirstWaitingQueueNumber(String line) {

        List<ProductionStage> waitingWashings =
                stageRepository.findByLineAndStageTypeAndCurrentStatusAndIsTemplate(
                        line,
                        StageType.WASHING,
                        StageStatus.NOT_YET,
                        false
                );

        return waitingWashings.stream()
                .map(stage -> {

                    try {

                        return queueClient.getQueueNumber(stage.getOrderId());

                    } catch (FeignException ex) {

                        throw new ServiceUnavailableException(
                                "Queue service is currently unavailable",
                                "QUEUE_SERVICE_DOWN"
                        );
                    }

                })
                .filter(q -> q != null)
                .min(Integer::compareTo)
                .orElse(null);
    }
}
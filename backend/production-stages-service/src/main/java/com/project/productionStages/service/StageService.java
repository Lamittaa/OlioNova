package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.dto.FinishStageRequest;
import com.project.productionStages.dto.StartStageRequest;
import com.project.productionStages.model.*;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.exception.ServiceUnavailableException;
import com.project.productionStages.repository.ProductionStageLoggingRepository;
import com.project.productionStages.repository.ProductionStageRepository;

import feign.FeignException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StageService {

        private final ProductionStageRepository stageRepository;
        private final ProductionStageLoggingRepository loggingRepository;
        private final OrderClient orderClient;

        public void startStage(StartStageRequest request) {

                ProductionStage stage = stageRepository.findById(request.getStageId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Stage not found with id: " + request.getStageId()));

                if (stage.getCurrentStatus() == StageStatus.DONE) {
                        throw new BusinessRuleException("Stage already finished");
                }

                if (stage.getCurrentStatus() == StageStatus.IN_PROGRESS) {
                        throw new BusinessRuleException("Stage already started");
                }

                if (stage.getStageOrder() > 1) {

                        ProductionStage prev = stageRepository.findByOrderItemIdAndStageOrder(
                                        stage.getOrderItemId(),
                                        stage.getStageOrder() - 1)
                                        .orElseThrow(() -> new ResourceNotFoundException("Previous stage not found"));

                        if (prev.getCurrentStatus() != StageStatus.DONE) {
                                throw new BusinessRuleException("Previous stage must finish first");
                        }
                }

                stage.setCurrentStatus(StageStatus.IN_PROGRESS);
                stageRepository.save(stage);

                ProductionStageLogging log = ProductionStageLogging.builder()
                                .name(stage.getName())
                                .stageType(stage.getStageType())
                                .orderId(stage.getOrderId())
                                .orderItemId(stage.getOrderItemId())
                                .line(stage.getLine())
                                .stageOrder(stage.getStageOrder())
                                .startTime(LocalDateTime.now())
                                .userId(request.getUserId())
                                .build();

                loggingRepository.save(log);
        }

        public void finishStage(FinishStageRequest request) {

                ProductionStage stage = stageRepository.findById(request.getStageId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Stage not found with id: " + request.getStageId()));

                if (stage.getCurrentStatus() == StageStatus.NOT_YET) {
                        throw new BusinessRuleException("Stage has not started yet");
                }

                if (stage.getCurrentStatus() == StageStatus.DONE) {
                        throw new BusinessRuleException("Stage already finished");
                }

                stage.setCurrentStatus(StageStatus.DONE);
                stageRepository.save(stage);

                ProductionStageLogging log = loggingRepository.findByOrderItemIdAndStageOrder(
                                stage.getOrderItemId(),
                                stage.getStageOrder())
                                .orElseThrow(() -> new ResourceNotFoundException("Stage log not found"));

                log.setEndTime(LocalDateTime.now());
                loggingRepository.save(log);

                if (stage.getStageType() == StageType.STORAGE) {

                        try {

                                orderClient.updateOrderStatus(
                                                stage.getOrderId(),
                                                "COMPLETED");

                        } catch (FeignException ex) {

                                throw new ServiceUnavailableException(
                                                "Order service is currently unavailable",
                                                "ORDER_SERVICE_DOWN");
                        }

                        return;
                }

                stageRepository.findByOrderItemIdAndStageOrder(
                                stage.getOrderItemId(),
                                stage.getStageOrder() + 1)
                                .ifPresent(next -> {
                                        next.setCurrentStatus(StageStatus.NOT_YET);
                                        stageRepository.save(next);
                                });
        }
}
package com.project.productionStages.service;

import com.project.productionStages.dto.FinishStageRequest;
import com.project.productionStages.dto.StartStageRequest;
import com.project.productionStages.model.*;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.repository.ProductionStageLoggingRepository;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StageService {

    private final ProductionStageRepository stageRepository;
    private final ProductionStageLoggingRepository loggingRepository;

    // =========================================================
    // START STAGE
    // =========================================================
    public void startStage(StartStageRequest request){

        ProductionStage stage =
                stageRepository.findById(request.getStageId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stage not found with id: " + request.getStageId()
                                )
                        );

        if(stage.getCurrentStatus() == StageStatus.DONE){
            throw new BusinessRuleException("Stage already finished");
        }

        if(stage.getCurrentStatus() == StageStatus.IN_PROGRESS){
            throw new BusinessRuleException("Stage already started");
        }

        // تحقق من المرحلة السابقة
        List<ProductionStage> stages =
                stageRepository.findByOrderItemId(stage.getOrderItemId());

        stages.stream()
                .filter(s -> s.getStageOrder() == stage.getStageOrder() - 1)
                .findFirst()
                .ifPresent(prev -> {

                    if(prev.getCurrentStatus() != StageStatus.DONE){
                        throw new BusinessRuleException(
                                "Previous stage must finish first"
                        );
                    }
                });

        stage.setCurrentStatus(StageStatus.IN_PROGRESS);

        stageRepository.save(stage);

        // تسجيل logging
        ProductionStageLogging log =
                ProductionStageLogging.builder()
                        .name(stage.getName())
                        .stageType(stage.getStageType())
                        .orderId(stage.getOrderId())
                        .orderItemId(stage.getOrderItemId())
                        .line(stage.getLine())
                        .stageOrder(stage.getStageOrder())
                        .startTime(LocalDateTime.now())
                        .employeeId(request.getEmployeeId())
                        .build();

        loggingRepository.save(log);
    }

    // =========================================================
    // FINISH STAGE
    // =========================================================
    public void finishStage(FinishStageRequest request){

        ProductionStage stage =
                stageRepository.findById(request.getStageId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stage not found with id: " + request.getStageId()
                                )
                        );

        if(stage.getCurrentStatus() == StageStatus.NOT_YET){
            throw new BusinessRuleException("Stage has not started yet");
        }

        if(stage.getCurrentStatus() == StageStatus.DONE){
            throw new BusinessRuleException("Stage already finished");
        }

        stage.setCurrentStatus(StageStatus.DONE);

        stageRepository.save(stage);

        // تحديث logging
        ProductionStageLogging log =
                loggingRepository
                        .findByOrderItemId(stage.getOrderItemId())
                        .stream()
                        .filter(l -> l.getStageOrder().equals(stage.getStageOrder()))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Stage log not found")
                        );

        log.setEndTime(LocalDateTime.now());

        loggingRepository.save(log);

        // تشغيل المرحلة التالية
        List<ProductionStage> stages =
                stageRepository.findByOrderItemId(stage.getOrderItemId());

        stages.stream()
                .filter(s -> s.getStageOrder() == stage.getStageOrder()+1)
                .findFirst()
                .ifPresent(next -> {

                    next.setCurrentStatus(StageStatus.NOT_YET);

                    stageRepository.save(next);
                });
    }

}
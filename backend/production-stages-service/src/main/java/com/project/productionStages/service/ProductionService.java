package com.project.productionStages.service;

import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.ProductionStageResponse;
import com.project.productionStages.dto.StartProductionRequest;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionStageRepository stageRepository;
    private final LineSelectionService lineSelectionService;
    private final QueueClient queueClient;

    // =========================================================
    // 1 START PRODUCTION
    // يُنشئ Order Stages من الـ Templates الموجودة في الخط
    // =========================================================
    public void startProduction(StartProductionRequest request) {

        String line = lineSelectionService.chooseBestLine();

        // ✅ جيب الـ Templates الخاصة بالخط بدل hard-coded pipeline
        List<ProductionStage> templates =
                stageRepository.findByLineAndIsTemplateOrderByStageOrderAsc(
                        line,
                        true
                );

        if (templates.isEmpty()) {
            throw new BusinessRuleException(
                    "No pipeline templates found for line: " + line
            );
        }

        // ✅ أنشئ Order Stage من كل Template — isTemplate = false
        for (ProductionStage template : templates) {

            ProductionStage orderStage = ProductionStage.builder()
                    .name(template.getName())
                    .stageType(template.getStageType())
                    .orderId(request.getOrderId())
                    .orderItemId(request.getOrderItemId())
                    .line(line)
                    .stageOrder(template.getStageOrder())
                    .currentStatus(StageStatus.NOT_YET)
                    .isTemplate(false) // ✅ order stage مش template
                    .build();

            stageRepository.save(orderStage);
        }

        // إصدار تذكرة Queue
        queueClient.issueProductionTicket(request.getOrderId());
    }

    // =========================================================
    // 2 GET STAGES OF ORDER
    // =========================================================
    public List<ProductionStageResponse> getStagesByOrder(Long orderItemId) {

        return stageRepository.findByOrderItemId(orderItemId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // 3 GET PIPELINE STATUS — order stages فقط
    // =========================================================
    public List<ProductionStageResponse> getPipelineStatus() {

        // ✅ بدل findAll().stream().filter(IN_PROGRESS)
        return stageRepository.findByCurrentStatusAndIsTemplate(
                        StageStatus.IN_PROGRESS,
                        false
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // 4 GET STAGE BY ID
    // =========================================================
    public ProductionStageResponse getStageById(Long id) {

        ProductionStage stage = stageRepository.findById(id)
                .orElseThrow(() ->
                        new com.project.productionStages.exception.ResourceNotFoundException(
                                "Stage not found with id: " + id
                        )
                );

        return mapToResponse(stage);
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private ProductionStageResponse mapToResponse(ProductionStage stage) {

        return ProductionStageResponse.builder()
                .id(stage.getId())
                .name(stage.getName())
                .stageType(stage.getStageType())
                .orderId(stage.getOrderId())
                .orderItemId(stage.getOrderItemId())
                .line(stage.getLine())
                .stageOrder(stage.getStageOrder())
                .currentStatus(stage.getCurrentStatus())
                .build();
    }
}
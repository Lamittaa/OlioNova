package com.project.productionStages.service;

import com.project.productionStages.dto.ProductionStageResponse;
import com.project.productionStages.dto.StartProductionRequest;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
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

        public void startProduction(StartProductionRequest request) {

                if (!stageRepository.findByOrderItemId(request.getOrderItemId()).isEmpty()) {
                        throw new BusinessRuleException(
                                        "Production already started for order item: "
                                                        + request.getOrderItemId());
                }

                String line = lineSelectionService.chooseBestLine();

                List<ProductionStage> templates = stageRepository.findByLineAndIsTemplateOrderByStageOrderAsc(
                                line,
                                true);

                if (templates.isEmpty()) {
                        throw new BusinessRuleException(
                                        "No pipeline templates found for line: " + line);
                }

                for (ProductionStage template : templates) {

                        ProductionStage orderStage = ProductionStage.builder()
                                        .name(template.getName())
                                        .stageType(template.getStageType())
                                        .orderId(request.getOrderId())
                                        .orderItemId(request.getOrderItemId())
                                        .line(line)
                                        .stageOrder(template.getStageOrder())
                                        .currentStatus(StageStatus.NOT_YET)
                                        .isTemplate(false)
                                        .build();

                        stageRepository.save(orderStage);
                }
        }

        public List<ProductionStageResponse> getStagesByOrder(Long orderItemId) {

                return stageRepository.findByOrderItemId(orderItemId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        public List<ProductionStageResponse> getPipelineStatus() {

                return stageRepository.findByCurrentStatusAndIsTemplate(
                                StageStatus.IN_PROGRESS,
                                false)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        public ProductionStageResponse getStageById(Long id) {

                ProductionStage stage = stageRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Stage not found with id: " + id));

                return mapToResponse(stage);
        }

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
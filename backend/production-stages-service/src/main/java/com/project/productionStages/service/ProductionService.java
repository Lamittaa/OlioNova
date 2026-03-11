package com.project.productionStages.service;

import com.project.productionStages.dto.ProductionStageResponse;
import com.project.productionStages.dto.StartProductionRequest;
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

    // =========================================================
    // 1️⃣ START PRODUCTION
    // =========================================================
    public void startProduction(StartProductionRequest request){

        String line = lineSelectionService.chooseBestLine();

        List<StageType> pipeline = List.of(
                StageType.WASHING,
                StageType.CRUSHING,
                StageType.MALAXATION,
                StageType.PRESSING,
                StageType.STORAGE
        );

        int order = 1;

        for(StageType stageType : pipeline){

            ProductionStage stage = ProductionStage.builder()
                    .name(stageType.name())
                    .stageType(stageType)
                    .orderId(request.getOrderId())
                    .orderItemId(request.getOrderItemId())
                    .line(line)
                    .stageOrder(order++)
                    .currentStatus(StageStatus.NOT_YET)
                    .build();

            stageRepository.save(stage);
        }
    }

    // =========================================================
    // 2️⃣ GET STAGES OF ORDER
    // =========================================================
    public List<ProductionStageResponse> getStagesByOrder(Long orderItemId){

        List<ProductionStage> stages =
                stageRepository.findByOrderItemId(orderItemId);

        return stages.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // 3️⃣ GET PIPELINE STATUS
    // =========================================================
    public List<ProductionStageResponse> getPipelineStatus(){

        List<ProductionStage> stages = stageRepository.findAll();

        return stages.stream()
                .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // 4️⃣ GET STAGE BY ID
    // =========================================================
    public ProductionStageResponse getStageById(Long id){

        ProductionStage stage =
                stageRepository.findById(id)
                        .orElseThrow();

        return mapToResponse(stage);
    }

    // =========================================================
    // 5️⃣ MAPPER (ENTITY → DTO)
    // =========================================================
    private ProductionStageResponse mapToResponse(ProductionStage stage){

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
package com.project.productionStages.controller;

import com.project.productionStages.dto.*;
import com.project.productionStages.service.ProductionService;
import com.project.productionStages.service.StageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService 
    uctionService;
    private final StageService stageService;

    // =========================================================
    // 1️⃣ START PRODUCTION (Receptionist sends order to production)
    // =========================================================
    @PostMapping("/start")
    public void startProduction(
            @Valid @RequestBody StartProductionRequest request
    ) {
        productionService.startProduction(request);
    }


    // =========================================================
    // 2️⃣ START STAGE (Technician starts stage)
    // =========================================================
    @PostMapping("/stages/start")
    public void startStage(
            @Valid @RequestBody StartStageRequest request
    ) {
        stageService.startStage(request);
    }


    // =========================================================
    // 3️⃣ FINISH STAGE (Technician finishes stage)
    // =========================================================
    @PostMapping("/stages/finish")
    public void finishStage(
            @Valid @RequestBody FinishStageRequest request
    ) {
        stageService.finishStage(request);
    }


    // =========================================================
    // 4️⃣ GET ALL STAGES OF ORDER
    // =========================================================
    @GetMapping("/order/{orderItemId}")
    public List<ProductionStageResponse> getOrderStages(
            @PathVariable Long orderItemId
    ) {
        return productionService.getStagesByOrder(orderItemId);
    }


    // =========================================================
    // 5️⃣ GET PIPELINE STATUS
    // =========================================================
    @GetMapping("/pipeline")
    public List<ProductionStageResponse> getPipelineStatus() {

        return productionService.getPipelineStatus();
    }


    // =========================================================
    // 6️⃣ GET STAGE BY ID
    // =========================================================
    @GetMapping("/stage/{id}")
    public ProductionStageResponse getStageById(
            @PathVariable Long id
    ) {
        return productionService.getStageById(id);
    }

}
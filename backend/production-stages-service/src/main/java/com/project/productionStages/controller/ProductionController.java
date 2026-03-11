package com.project.productionStages.controller;

import com.project.productionStages.dto.*;
import com.project.productionStages.service.ProductionDashboardService;
import com.project.productionStages.service.ProductionEtaService;
import com.project.productionStages.service.ProductionService;
import com.project.productionStages.service.StageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

private final ProductionDashboardService dashboardService;
private final ProductionService productionService;
private final StageService stageService;
private final ProductionEtaService etaService;

    // =========================================================
    // 1️⃣ START PRODUCTION
    // RECEPTIONIST
    // =========================================================
    @PostMapping("/start")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public void startProduction(
            @Valid @RequestBody StartProductionRequest request
    ) {
        productionService.startProduction(request);
    }

    // =========================================================
    // 2️⃣ START STAGE
    // TECHNICIAN
    // =========================================================
    @PostMapping("/stages/start")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void startStage(
            @Valid @RequestBody StartStageRequest request
    ) {
        stageService.startStage(request);
    }

    // =========================================================
    // 3️⃣ FINISH STAGE
    // TECHNICIAN
    // =========================================================
    @PostMapping("/stages/finish")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void finishStage(
            @Valid @RequestBody FinishStageRequest request
    ) {
        stageService.finishStage(request);
    }

    // =========================================================
    // 4️⃣ GET ORDER STAGES
    // ADMIN + TECHNICIAN
    // =========================================================
    @GetMapping("/order/{orderItemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public List<ProductionStageResponse> getOrderStages(
            @PathVariable Long orderItemId
    ) {
        return productionService.getStagesByOrder(orderItemId);
    }

    // =========================================================
    // 5️⃣ PIPELINE STATUS
    // ADMIN + TECHNICIAN
    // =========================================================
    @GetMapping("/pipeline")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public List<ProductionStageResponse> getPipelineStatus() {

        return productionService.getPipelineStatus();
    }

    // =========================================================
    // 6️⃣ GET STAGE
    // ADMIN + TECHNICIAN
    // =========================================================
    @GetMapping("/stage/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ProductionStageResponse getStageById(
            @PathVariable Long id
    ) {
        return productionService.getStageById(id);
    }

    // =========================================================
    // 7️⃣ DASHBOARD
    // ADMIN + ACCOUNTANT
    // =========================================================
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public List<ProductionDashboardResponse> getDashboard(){

        return dashboardService.getDashboard();
    }

    // =========================================================
    // 8️⃣ ETA
    // ADMIN + RECEPTIONIST
    // =========================================================
    @GetMapping("/eta/{orderItemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ProductionEtaResponse getEta(
            @PathVariable Long orderItemId
    ){
        return etaService.getEta(orderItemId);
    }
}
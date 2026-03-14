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

    @PostMapping("/start")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public void startProduction(
            @Valid @RequestBody StartProductionRequest request) {
        productionService.startProduction(request);
    }

    @PostMapping("/stages/start")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void startStage(
            @Valid @RequestBody StartStageRequest request) {
        stageService.startStage(request);
    }

    @PostMapping("/stages/finish")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN')")
    public void finishStage(
            @Valid @RequestBody FinishStageRequest request) {
        stageService.finishStage(request);
    }

    @GetMapping("/order/{orderItemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public List<ProductionStageResponse> getOrderStages(
            @PathVariable Long orderItemId) {
        return productionService.getStagesByOrder(orderItemId);
    }

    @GetMapping("/pipeline")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public List<ProductionStageResponse> getPipelineStatus() {

        return productionService.getPipelineStatus();
    }

    @GetMapping("/stage/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    public ProductionStageResponse getStageById(
            @PathVariable Long id) {
        return productionService.getStageById(id);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    public List<ProductionDashboardResponse> getDashboard() {

        return dashboardService.getDashboard();
    }

    @GetMapping("/eta/{orderItemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ProductionEtaResponse getEta(
            @PathVariable Long orderItemId) {
        return etaService.getEta(orderItemId);
    }
}
package com.project.productionStages.controller;

import com.project.productionStages.dto.*;
import com.project.productionStages.model.StageType;
import com.project.productionStages.service.OrderManagementService;
import com.project.productionStages.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final OrderManagementService orderManagementService;

    // =========================================================
    // ✅ 1. Get Orders List
    // =========================================================
    @GetMapping("/orders")
    public ResponseEntity<List<ProductionDashboardDto>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(orderManagementService.getDashboardFiltered(sort, status));
    }

    // =========================================================
    // ✅ 2. Get Order Details
    // =========================================================
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(productionService.getOrderDetails(orderId));
    }

    @PutMapping("/items/{itemId}/change-stage")
    public ResponseEntity<MoveStageResponse> changStage(
            @PathVariable Long itemId,
            @RequestParam String stageType,
            @RequestParam String container) {

        StageType stageTypeEnum = StageType.valueOf(stageType.toUpperCase());

        MoveStageResponse response = productionService.changeStage(
                itemId,
                stageTypeEnum,
                container);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/start")
    public ResponseEntity<String> startProduction(
            @RequestBody StartProductionRequest request) {

        productionService.startProduction(request);

        return ResponseEntity.ok("Production started");
    }

    @GetMapping("/line/stage")
    public ResponseEntity<List<StageGroupResponse>> getStagesByLine(
            @RequestParam String line) {
        return ResponseEntity.ok(productionService.getStagesByLine(line));
    }

    // =========================================================
    // ✅ GET available lines
    // =========================================================
    @GetMapping("/lines/available")
    public ResponseEntity<List<String>> getAvailableLines() {

        List<String> lines = productionService.getAvailableLines();

        return ResponseEntity.ok(lines);
    }

    @GetMapping("/lines/overview")
    public ResponseEntity<List<LineResponse>> getLinesOverview() {
        return ResponseEntity.ok(productionService.getLineOverview());
    }

    @PostMapping("/storage/deliver")
    public ResponseEntity<Void> markStorageDelivered(@RequestBody List<Long> stageIds ){

         productionService.markStorageDelivered(stageIds);
        return ResponseEntity.noContent().build();
    }
}
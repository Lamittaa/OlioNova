package com.project.productionStages.controller;

import com.project.productionStages.dto.*;
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

    // =========================================================
    // ✅ 1. Get Orders List
    // =========================================================
    @GetMapping("/orders")
    public ResponseEntity<List<ProductionOrderListResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(productionService.getOrders(status, sort));
    }

    // =========================================================
    // ✅ 2. Get Order Details
    // =========================================================
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(productionService.getOrderDetails(orderId));
    }

    // =========================================================
    // ✅ 3. Next Stage
    // =========================================================
    @PutMapping("/items/{itemId}/next-stage")
    public ResponseEntity<Void> moveToNextStage(
            @PathVariable Long itemId) {
        productionService.moveToNextStage(itemId);
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // ✅ 4. Change Stage (dropdown)
    // =========================================================
    @PutMapping("/items/{itemId}/stage")
    public ResponseEntity<Void> changeStage(
            @PathVariable Long itemId,
            @RequestBody ChangeStageRequest request) {
        productionService.changeStage(itemId, request.getStageType());
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // ✅ 5. Assign Line (A / B)
    // =========================================================
    @PutMapping("/items/{itemId}/assign-line")
    public ResponseEntity<Void> assignLine(
            @PathVariable Long itemId,
            @RequestBody AssignLineRequest request) {
        productionService.assignLine(itemId, request.getLine());
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // ✅ 6. Assign Container (عجين / تخزين)
    // =========================================================
    @PutMapping("/items/{itemId}/assign-container")
    public ResponseEntity<Void> assignContainer(
            @PathVariable Long itemId,
            @RequestBody AssignContainerRequest request) {
        productionService.assignContainer(itemId, request.getContainer());
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // ✅ 7. Get Production Lines (🔥 أهم API)
    // =========================================================
    @GetMapping("/lines")
    public ResponseEntity<List<LineResponse>> getLines() {
        return ResponseEntity.ok(productionService.getLines());
    }

    @PostMapping("/start")
    public ResponseEntity<String> startProduction(
            @RequestParam Long orderItemId,
            @RequestParam Long orderId // ← الإضافة
    ) {
        productionService.startProduction(orderItemId, orderId);
        return ResponseEntity.ok("Production started");
    }
}
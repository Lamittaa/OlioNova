package com.project.productionStages.controller;

import com.project.productionStages.dto.CreateProductionBatchRequest;
import com.project.productionStages.dto.ProductionBatchResponse;
import com.project.productionStages.dto.UpdateBatchPredictionRequest;
import com.project.productionStages.service.ProductionBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/production/batches")
@RequiredArgsConstructor
public class ProductionBatchController {

    private final ProductionBatchService productionBatchService;

    @PostMapping
    public ResponseEntity<ProductionBatchResponse> createBatch(
            @Valid @RequestBody CreateProductionBatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionBatchService.createBatch(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductionBatchResponse>> getBatches() {
        return ResponseEntity.ok(productionBatchService.getBatches());
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<ProductionBatchResponse> getBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(productionBatchService.getBatch(batchId));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ProductionBatchResponse> getBatchByOrderItemId(@PathVariable Long orderItemId) {
        return ResponseEntity.ok(productionBatchService.getBatchByOrderItemId(orderItemId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ProductionBatchResponse> getBatchByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(productionBatchService.getBatchByOrderId(orderId));
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<ProductionBatchResponse> createOrGetBatchByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(productionBatchService.createOrGetBatchByOrderId(orderId));
    }

    @PutMapping("/{batchId}/prediction")
    public ResponseEntity<ProductionBatchResponse> updatePrediction(
            @PathVariable String batchId,
            @RequestBody UpdateBatchPredictionRequest request
    ) {
        return ResponseEntity.ok(productionBatchService.updatePrediction(batchId, request));
    }
}

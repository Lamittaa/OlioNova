package com.ops.tracking_service.controller;

import com.ops.tracking_service.dto.TrackingResponse;
import com.ops.tracking_service.dto.UpdateProductionLineRequest;
import com.ops.tracking_service.dto.UpdateTankRequest;
import com.ops.tracking_service.dto.UpdateTrackingStatusRequest;
import com.ops.tracking_service.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<TrackingResponse> getByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(trackingService.getByBatchId(batchId));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<TrackingResponse> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(trackingService.getByOrderId(orderId));
    }

    @PutMapping("/batches/{batchId}/status")
    public ResponseEntity<TrackingResponse> updateStatus(
            @PathVariable String batchId,
            @Valid @RequestBody UpdateTrackingStatusRequest request
    ) {
        return ResponseEntity.ok(trackingService.updateStatus(batchId, request.getStatus()));
    }

    @PutMapping("/batches/{batchId}/tank")
    public ResponseEntity<TrackingResponse> updateTank(
            @PathVariable String batchId,
            @Valid @RequestBody UpdateTankRequest request
    ) {
        return ResponseEntity.ok(trackingService.updateTank(batchId, request.getTankCode()));
    }

    @PutMapping("/batches/{batchId}/line")
    public ResponseEntity<TrackingResponse> updateProductionLine(
            @PathVariable String batchId,
            @Valid @RequestBody UpdateProductionLineRequest request
    ) {
        return ResponseEntity.ok(trackingService.updateProductionLine(batchId, request.getProductionLine()));
    }
}

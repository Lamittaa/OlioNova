package com.ops.tracking_service.controller;

import com.ops.tracking_service.dto.TrackingResponse;
import com.ops.tracking_service.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tracking")
public class PublicTrackingController {

    private final TrackingService trackingService;

    public PublicTrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<TrackingResponse> getByTrackingCode(@PathVariable String trackingCode) {
        return ResponseEntity.ok(trackingService.getByTrackingCode(trackingCode));
    }
}

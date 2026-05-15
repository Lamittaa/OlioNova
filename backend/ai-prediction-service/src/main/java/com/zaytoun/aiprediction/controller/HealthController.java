package com.zaytoun.aiprediction.controller;

import com.zaytoun.aiprediction.dto.HealthResponse;
import com.zaytoun.aiprediction.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping("/health")
    public HealthResponse health() {
        return healthService.getHealth();
    }
}

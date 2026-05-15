package com.zaytoun.aiprediction.controller;

import com.zaytoun.aiprediction.dto.TrainingJobResponse;
import com.zaytoun.aiprediction.dto.TrainingRequest;
import com.zaytoun.aiprediction.dto.TrainingStatusResponse;
import com.zaytoun.aiprediction.service.TrainingJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/train")
@RequiredArgsConstructor
public class TrainingController {
    private final TrainingJobService trainingJobService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TrainingJobResponse train(@Valid @RequestBody(required = false) TrainingRequest request) {
        TrainingRequest payload = request == null ? new TrainingRequest() : request;
        return TrainingJobResponse.from(trainingJobService.queueTraining(payload));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TrainingStatusResponse getStatus(@PathVariable String jobId) {
        return TrainingStatusResponse.from(trainingJobService.getStatus(jobId));
    }
}

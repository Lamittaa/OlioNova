package com.zaytoun.aiprediction.controller;

import com.zaytoun.aiprediction.dto.PredictionResponse;
import com.zaytoun.aiprediction.service.PredictionService;
import com.zaytoun.aiprediction.validation.BatchIdRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class PredictionController {
    private final PredictionService predictionService;

    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PredictionResponse predict(@RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false)
                                      @Size(max = 64, message = BatchIdRules.MESSAGE)
                                      @Pattern(regexp = BatchIdRules.REGEX, message = BatchIdRules.MESSAGE)
                                      String batchId,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) String cultivar,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestDate) {
        if (batchId != null && !batchId.isBlank()) {
            return predictionService.predict(file, batchId, cultivar, harvestDate);
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        return predictionService.predictByOrderId(file, orderId, cultivar, harvestDate);
    }
}

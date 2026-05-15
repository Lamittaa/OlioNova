package com.zaytoun.aiclient;

import com.zaytoun.aiclient.dto.PredictionResponse;
import com.zaytoun.aiclient.dto.UpdateYieldRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "AI-PREDICTION-SERVICE")
public interface OliveAiPredictionClient {

    @PostMapping(value = "/api/v1/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    PredictionResponse predict(@RequestHeader("X-API-Key") String apiKey,
                               @RequestPart("file") MultipartFile file,
                               @RequestParam("batchId") String batchId,
                               @RequestParam(value = "cultivar", required = false) String cultivar,
                               @RequestParam(value = "harvestDate", required = false) String harvestDate);

    @PutMapping("/api/v1/dataset/{batchId}/yield")
    void updateYield(@RequestHeader("X-API-Key") String apiKey,
                     @PathVariable("batchId") String batchId,
                     @RequestBody UpdateYieldRequest request);
}

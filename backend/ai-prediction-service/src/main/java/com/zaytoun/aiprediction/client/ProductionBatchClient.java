package com.zaytoun.aiprediction.client;

import com.zaytoun.aiprediction.config.InternalApiKeyFeignConfig;
import com.zaytoun.aiprediction.dto.ProductionBatchResponse;
import com.zaytoun.aiprediction.dto.UpdateBatchPredictionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "productionStages-service",
        url = "${clients.production-stages.url:http://localhost:9030}",
        configuration = InternalApiKeyFeignConfig.class
)
public interface ProductionBatchClient {

    @GetMapping("/api/production/batches/{batchId}")
    ProductionBatchResponse getBatch(@PathVariable("batchId") String batchId);

    @PostMapping("/api/production/batches/order/{orderId}")
    ProductionBatchResponse createOrGetBatchByOrderId(@PathVariable("orderId") Long orderId);

    @PutMapping("/api/production/batches/{batchId}/prediction")
    ProductionBatchResponse updatePrediction(
            @PathVariable("batchId") String batchId,
            @RequestBody UpdateBatchPredictionRequest request
    );
}

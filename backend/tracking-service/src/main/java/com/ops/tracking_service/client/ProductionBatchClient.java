package com.ops.tracking_service.client;

import com.ops.tracking_service.dto.ProductionBatchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "productionStages-service", configuration = com.ops.tracking_service.config.FeignAuthForwardConfig.class)
public interface ProductionBatchClient {

    @GetMapping("/api/production/batches/{batchId}")
    ProductionBatchResponse getBatch(@PathVariable String batchId);

    @GetMapping("/api/production/batches/order/{orderId}")
    ProductionBatchResponse getBatchByOrderId(@PathVariable Long orderId);
}

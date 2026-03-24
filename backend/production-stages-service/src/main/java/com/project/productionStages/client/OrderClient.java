package com.project.productionStages.client;

import com.project.productionStages.config.FeignAuthForwardConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        name = "order-service",
        configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    @PutMapping("/api/orders/{orderId}/status")
    void updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody Map<String, String> body  // ← غير من RequestParam لـ RequestBody
    );
}
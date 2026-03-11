package com.project.queue_service.client;

import com.project.queue_service.config.FeignAuthForwardConfig;
import com.project.queue_service.dto.OrderResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "order-service",
        configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    // ============================================
    // 1️⃣ GET ORDER BY ID
    // ============================================
    @GetMapping("/api/orders/{orderId}")
    OrderResponse getOrderById(
            @PathVariable Long orderId
    );

   


}
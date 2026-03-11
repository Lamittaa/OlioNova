package com.project.productionStages.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PutMapping("/api/orders/{orderId}/status")
    void updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status
    );

}
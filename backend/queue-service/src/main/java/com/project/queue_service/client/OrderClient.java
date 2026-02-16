package com.project.queue_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PutMapping("/api/orders/{id}/status")
    void updateOrderStatus(
            @PathVariable("id") Long orderId,
            @RequestBody Map<String, String> body
    );
}

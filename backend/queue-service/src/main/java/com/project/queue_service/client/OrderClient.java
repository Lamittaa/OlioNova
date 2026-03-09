package com.project.queue_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.project.queue_service.config.FeignAuthForwardConfig;
import com.project.queue_service.dto.OrderItemResponse;
import com.project.queue_service.dto.OrderResponse;

import java.util.Map;

@FeignClient(
        name = "order-service",
        configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    @GetMapping("/api/orders/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId);

    @GetMapping("/api/orders/{orderId}/items/{itemId}")
    OrderItemResponse getOrderItemById(
            @PathVariable Long orderId,
            @PathVariable Long itemId
    );

    @PutMapping("/api/orders/{id}/status")
    void updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    );

    @PutMapping("/api/orders/items/{itemId}/status")
    void updateOrderItemStatus(
            @PathVariable Long itemId,
            @RequestBody Map<String, String> body
    );
}
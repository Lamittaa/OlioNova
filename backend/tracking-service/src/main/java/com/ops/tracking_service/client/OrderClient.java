package com.ops.tracking_service.client;

import com.ops.tracking_service.config.FeignAuthForwardConfig;
import com.ops.tracking_service.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", configuration = FeignAuthForwardConfig.class)
public interface OrderClient {

    @GetMapping("/api/orders/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId);
}

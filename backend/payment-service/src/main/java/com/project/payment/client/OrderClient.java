package com.project.payment.client;

import com.project.payment.config.FeignAuthForwardConfig;
import com.project.payment.dto.OrderSummaryResponse;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "ORDER-SERVICE",
    configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    OrderSummaryResponse getOrderById(@PathVariable Long id);

    @PutMapping("/api/orders/{id}/status")
    void updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    );

    @PostMapping("/api/orders/{id}/pay")
void payOrder(@PathVariable Long id);
}

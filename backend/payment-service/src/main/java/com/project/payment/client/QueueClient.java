package com.project.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "queue-service")
public interface QueueClient {

    @PostMapping("/api/queues/production")
    void addToProduction(@RequestParam Long orderId);
}

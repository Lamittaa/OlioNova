package com.project.productionStages.client;

import com.project.productionStages.config.FeignAuthForwardConfig;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "queue-service",
        configuration = FeignAuthForwardConfig.class
)
public interface QueueClient {

    // =========================================
    // ISSUE PRODUCTION TICKET
    // =========================================
    @PostMapping("/api/queues/production/{orderId}")
    void issueProductionTicket(
            @PathVariable("orderId") Long orderId
    );

    // =========================================
    // GET QUEUE NUMBER FOR ORDER
    // =========================================
    @GetMapping("/api/queues/order/{orderId}")
    Integer getQueueNumber(
            @PathVariable("orderId") Long orderId
    );
}
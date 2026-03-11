package com.project.payment.client;

import com.project.payment.config.FeignAuthForwardConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "queue-service",
        configuration = FeignAuthForwardConfig.class
)
public interface QueueClient {

    @PostMapping("/api/queues/production/{orderId}")
    void issueProductionTicket(
            @PathVariable Long orderId
    );

}
package com.project.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "queue-service"
)
public interface QueueClient {

    @PostMapping("/api/queues/accounting/tickets")
    void issueAccountingTicket(
            @RequestParam Long orderId
    );
}
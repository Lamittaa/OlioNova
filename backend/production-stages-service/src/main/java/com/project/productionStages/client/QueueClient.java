package com.project.productionStages.client;

import com.project.productionStages.config.FeignAuthForwardConfig;
import com.project.productionStages.dto.QueueTicketResponse;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "queue-service",
    configuration = FeignAuthForwardConfig.class
)
public interface QueueClient {

    @GetMapping("/api/queues/order/{orderId}")
    Integer getQueueNumber(
            @PathVariable("orderId") Long orderId
    );

     @GetMapping("/api/queues/tickets")
    List<QueueTicketResponse> getTicketsByQueueType(
            @RequestParam String queueType,
            @RequestParam LocalDate date
    );
}
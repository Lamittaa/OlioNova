package com.project.queue_service.controller;
import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TellerAction;
import com.project.queue_service.service.QueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueManager queueManager;

    // إصدار رقم جديد
    @PostMapping("/{queueType}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueTicket issueTicket(@PathVariable String queueType) {
        return queueManager.issueTicket(queueType);
    }

    // NEXT أو SKIP
    @PostMapping("/{queueType}/advance")
    public QueueTicket advanceQueue(
            @PathVariable String queueType,
            @RequestParam Long tellerId,
            @RequestParam TellerAction action
    ) {
        return queueManager.advanceQueue(queueType, tellerId, action);
    }

    // عرض حالة الطابور
    @GetMapping("/{queueType}/status")
    public QueueResponseDto getStatus(@PathVariable String queueType) {
        return queueManager.getQueueStatus(queueType);
    }

@PostMapping("/production")
@ResponseStatus(HttpStatus.CREATED)
public QueueTicket addToProduction(@RequestParam Long orderId) {
    return queueManager.addToProduction(orderId);
}

@PostMapping("/{queueType}/complete")
public QueueTicket completeTicket(
        @PathVariable String queueType,
        @RequestParam Long ticketId
) {
    return queueManager.completeTicket(queueType, ticketId);
}


}

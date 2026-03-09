package com.project.queue_service.controller;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.QueueType;
import com.project.queue_service.model.TellerAction;
import com.project.queue_service.service.QueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueManager queueManager;

    // ================= ISSUE TICKET =================
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
    @PostMapping("/{queueType}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueTicket issueTicket(
            @PathVariable QueueType queueType) {
        return queueManager.issueTicket(queueType);
    }

    // ================= ADVANCE =================
    @PreAuthorize("hasAnyRole('ACCOUNTANT','TECHNICIAN','ADMIN')")
    @PostMapping("/{queueType}/advance")
    public QueueTicket advanceQueue(
            @PathVariable QueueType queueType,
            @RequestParam TellerAction action) {

        return queueManager.advanceQueue(queueType, action);
    }

    // ================= STATUS =================
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{queueType}/status")
    public QueueResponseDto getStatus(
            @PathVariable QueueType queueType) {
        return queueManager.getQueueStatus(queueType);
    }

    // ================= ADD TO PRODUCTION =================
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
  @PostMapping("/production")
public QueueTicket addToProduction(
        @RequestParam Long orderId,
        @RequestParam Long orderItemId) {

    return queueManager.addToProduction(orderId, orderItemId);
}

    // ================= COMPLETE =================
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    @PostMapping("/{queueType}/complete")
    public QueueTicket completeTicket(
            @PathVariable QueueType queueType,
            @RequestParam Long ticketId) {
        return queueManager.completeTicket(queueType, ticketId);
    }

}
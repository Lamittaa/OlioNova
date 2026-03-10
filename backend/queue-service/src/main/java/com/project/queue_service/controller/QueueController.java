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
    @PostMapping("/accounting/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueTicket issueTicket(
            @RequestParam Long orderId) {
        return queueManager.issueTicket(QueueType.ACCOUNTING,orderId);
    
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




}
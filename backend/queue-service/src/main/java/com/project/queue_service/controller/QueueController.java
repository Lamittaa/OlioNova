package com.project.queue_service.controller;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueTicketResponse;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.QueueType;
import com.project.queue_service.model.TellerAction;
import com.project.queue_service.service.QueueManager;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
        return queueManager.issueTicket(QueueType.ACCOUNTING, orderId);

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

    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    @PostMapping("/production/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueTicket issueProductionTicket(
            @PathVariable Long orderId) {

        return queueManager.issueTicket(
                QueueType.PRODUCTION,
                orderId);
    }

    @GetMapping("/tickets")
    public List<QueueTicketResponse> getTicketsByQueueType(
            @RequestParam String queueType,
            @RequestParam LocalDate date) {
        return queueManager.getTicketsByQueueType(
                QueueType.valueOf(queueType.toUpperCase()),
                date);
    }

    @GetMapping("/{queueType}/order/{orderId}")
    Integer getQueueNumber(@PathVariable("orderId") Long orderId, @PathVariable String queueType) {

        return queueManager.getTicketNumberByOrderIdAndQueueType(orderId, queueType);

    }


}

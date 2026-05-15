package com.project.queue_service.controller;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueTicketResponse;
import com.project.queue_service.mapper.QueueTicketMapper;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.QueueType;
import com.project.queue_service.model.TellerAction;
import com.project.queue_service.model.TicketStatus;
import com.project.queue_service.service.QueueManager;
import com.project.queue_service.service.TellerSessionService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

        private final QueueManager queueManager;
        private final TellerSessionService tellerSessionService;

        public QueueController(QueueManager queueManager, TellerSessionService tellerSessionService) {
                this.queueManager = queueManager;
                this.tellerSessionService = tellerSessionService;
        }

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

        @PutMapping("/tickets/{ticketId}/status")
        ResponseEntity<QueueTicketResponse> updateStatus(
                        @PathVariable Long ticketId,
                        @RequestParam String queueType,
                        @RequestParam String status,
                        @RequestParam(required = false) String productionLine) {

                TicketStatus statusE = TicketStatus.valueOf(status.toUpperCase());
                QueueType queueTypeE = QueueType.valueOf(queueType.toUpperCase());
                QueueTicket updated = queueManager.updateTicketStatus(queueTypeE, ticketId, null, statusE, productionLine);

                return ResponseEntity.ok(
                                QueueTicketMapper.mapToResponse(updated));
        }

        @PutMapping("/tickets/status")
        ResponseEntity<QueueTicketResponse> updateStatus(
                        @RequestParam(required = false) Long ticketId,
                        @RequestParam(required = false) Long orderId,
                        @RequestParam String queueType,
                        @RequestParam String status,
                        @RequestParam(required = false) String productionLine) {

                TicketStatus statusE = TicketStatus.valueOf(status.toUpperCase());
                QueueType queueTypeE = QueueType.valueOf(queueType.toUpperCase());
                QueueTicket updated = queueManager.updateTicketStatus(queueTypeE, ticketId, orderId, statusE, productionLine);

                return ResponseEntity.ok(
                                QueueTicketMapper.mapToResponse(updated));
        }

        @PostMapping("/tickets/{ticketId}/return")
        public QueueTicket returnTicket(@PathVariable Long ticketId) {
                return queueManager.returnToQueue(ticketId);
        }

        @PreAuthorize("hasAnyRole('ACCOUNTANT','TECHNICIAN','ADMIN')")
        @PostMapping("/{queueType}/recall")
        public ResponseEntity<QueueTicketResponse> recall(
                        @PathVariable QueueType queueType) {

                QueueTicket ticket = queueManager.recallMissedTicket(queueType);

                return ResponseEntity.ok(
                                QueueTicketMapper.mapToResponse(ticket));
        }

        @PreAuthorize("hasAnyRole('ACCOUNTANT','TECHNICIAN','ADMIN')")
        @PostMapping("/{queueType}/recall-current")
        public ResponseEntity<QueueTicketResponse> recallCurrent(
                        @PathVariable QueueType queueType) {

                QueueTicket ticket = queueManager.recallCurrent(queueType);

                return ResponseEntity.ok(
                                QueueTicketMapper.mapToResponse(ticket));
        }


        @PostMapping("/teller/login")
public ResponseEntity<String> loginTeller(
        @RequestParam QueueType queueType) {

    Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long userId = jwt.getClaim("userId");

    tellerSessionService.login(userId, queueType);

    return ResponseEntity.ok("Teller logged in");
}

@PostMapping("/teller/logout")
public ResponseEntity<String> logoutTeller(
        @RequestParam QueueType queueType) {

    Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Long userId = jwt.getClaim("userId");

    tellerSessionService.logout(userId, queueType);

    return ResponseEntity.ok("Teller logged out");
}
}

package com.project.queue_service.service;

import com.project.queue_service.dto.PublicQueueDisplayResponse;
import com.project.queue_service.dto.PublicQueueTicketDto;
import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueTicketResponse;
import com.project.queue_service.dto.QueueUpdatedEvent;
import com.project.queue_service.dto.ServingTicketDto;
import com.project.queue_service.exception.*;
import com.project.queue_service.mapper.QueueTicketMapper;
import com.project.queue_service.model.*;
import com.project.queue_service.repository.QueueTicketRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class QueueManager {

    private static final Logger log = LoggerFactory.getLogger(QueueManager.class);

    private final QueueTicketRepo queueTicketRepo;
    private final QueueCounterService queueCounterService;
    private final ApplicationEventPublisher appEventPublisher;
    private final TellerSessionService tellerSessionService;

    public QueueManager(
            QueueTicketRepo queueTicketRepo,
            QueueCounterService queueCounterService,
            ApplicationEventPublisher appEventPublisher,
            TellerSessionService tellerSessionService) {
        this.queueTicketRepo = queueTicketRepo;
        this.queueCounterService = queueCounterService;
        this.appEventPublisher = appEventPublisher;
        this.tellerSessionService = tellerSessionService;
    }

    @Transactional
    public QueueTicket issueTicket(QueueType queueType, Long orderId) {

        // ✅ منع إنشاء تذكرة مكررة لنفس الطلب
        if (queueTicketRepo.existsByOrderIdAndQueueType(orderId, queueType)) {

            throw new DuplicateTicketException(
                    "Ticket already exists for this order in " + queueType);
        }

        int nextNumber = queueCounterService.getAndIncrementTicketNumber(queueType);

        QueueTicket ticket = new QueueTicket();
        ticket.setQueueType(queueType);
        ticket.setQueueDate(LocalDate.now());
        ticket.setTicketNumber(nextNumber);
        ticket.setTicketStatus(TicketStatus.WAITING);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setOrderId(orderId);

        QueueTicket saved = queueTicketRepo.save(ticket);

        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return saved;
    }

    @Transactional(noRollbackFor = NoWaitingTicketException.class)
    public QueueTicket advanceQueue(
            QueueType queueType,
            TellerAction action) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication");
        }

        Long userId = jwt.getClaim("userId");
        if (userId == null) {
            throw new AccessDeniedException("userId missing in token");
        }

        if (!tellerSessionService.isActive(userId, queueType)) {
            throw new AccessDeniedException("Teller not logged in");
        }

        // 1️⃣ check current serving ticket
        Optional<QueueTicket> servingTicket = Optional.empty();
        if (queueType != QueueType.PRODUCTION) {
            servingTicket = queueTicketRepo.findByUserIdAndTicketStatusAndQueueDate(
                    userId,
                    TicketStatus.SERVING,
                    LocalDate.now());

            servingTicket.ifPresent(t -> postServingTicket(t, action));
        }

        // 2️⃣ get next waiting ticket
        Optional<QueueTicket> next = queueTicketRepo.findNextWaiting(queueType);

        if (next.isEmpty()) {

            // إذا أنهينا تذكرة فقط لا نحتاج فتح أخرى
            if (servingTicket.isPresent()) {
                appEventPublisher.publishEvent(
                        new QueueUpdatedEvent(queueType));
            }

            throw new NoWaitingTicketException(queueType);
        }

        QueueTicket t = next.get();

        t.setUserId(userId);
        t.setCalledAt(LocalDateTime.now());
        t.setTicketStatus(TicketStatus.SERVING);

        QueueTicket saved = queueTicketRepo.save(t);

        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return saved;
    }

    private QueueTicket postServingTicket(
            QueueTicket t,
            TellerAction action) {

        TicketStatus status;

        if (action == TellerAction.NEXT) {

            status = TicketStatus.COMPLETED;

        } else {

            status = TicketStatus.MISSED; // 🔥 فقط عند SKIP
            t.setMissedAt(LocalDateTime.now()); // 🔥 هون فقط

        }

        t.setTicketStatus(status);
        t.setCompletedAt(LocalDateTime.now());

        return queueTicketRepo.save(t);
    }

    // @Transactional
    // public QueueTicket completeTicket(
    // QueueType queueType,
    // Long ticketId) {

    // QueueTicket ticket =
    // queueTicketRepo.findById(ticketId)
    // .orElseThrow(() ->
    // new TicketNotFoundException(ticketId));

    // if (ticket.getTicketStatus() != TicketStatus.SERVING) {
    // throw new InvalidTicketStateException(
    // "Only SERVING ticket can be completed");
    // }

    // ticket.setTicketStatus(TicketStatus.COMPLETED);
    // ticket.setCompletedAt(LocalDateTime.now());

    // QueueTicket saved = queueTicketRepo.save(ticket);

    // if (queueType == QueueType.PRODUCTION
    // && saved.getOrderItemId() != null) {

    // orderClient.updateOrderItemStatus(
    // saved.getOrderItemId(),
    // Map.of("status", "READY_FOR_PICKUP"));
    // }

    // appEventPublisher.publishEvent(
    // new QueueUpdatedEvent(queueType));

    // return saved;
    // }

    public QueueResponseDto getQueueStatus(
            QueueType queueType) {
        try {
            List<QueueTicket> tickets = queueTicketRepo
                    .findAllByQueueTypeAndQueueDateAndTicketStatusIn(
                            queueType,
                            LocalDate.now(),
                            List.of(
                                    TicketStatus.SERVING,
                                    TicketStatus.WAITING));

            return QueueDtoUtil.buildQueueResponse(tickets);
        } catch (Exception ex) {
            log.error("Failed to build queue status for {}", queueType, ex);
            return QueueDtoUtil.buildQueueResponse(List.of());
        }
    }

    public PublicQueueDisplayResponse getPublicQueueDisplay(QueueType queueType) {
        QueueResponseDto status = getQueueStatus(queueType);
        PublicQueueTicketDto nowServing = status.serving().stream()
                .findFirst()
                .map(ticket -> new PublicQueueTicketDto(
                        displayTicketNumber(queueType, ticket.ticket().number()),
                        ticket.startedAt(),
                        ticket.ticket().orderId(),
                        0,
                        ticket.ticket().productionLine()
                ))
                .orElse(null);

        List<PublicQueueTicketDto> nowServingLines = status.serving().stream()
                .sorted(Comparator
                        .comparingInt((ServingTicketDto ticket) -> lineOrder(ticket.ticket().productionLine()))
                        .thenComparingInt(ticket -> ticketNumberOrder(ticket.ticket().number())))
                .map(ticket -> new PublicQueueTicketDto(
                        displayTicketNumber(queueType, ticket.ticket().number()),
                        ticket.startedAt(),
                        ticket.ticket().orderId(),
                        0,
                        ticket.ticket().productionLine()
                ))
                .toList();

        List<PublicQueueTicketDto> nextInLine = status.waiting().stream()
                .limit(8)
                .map(ticket -> new PublicQueueTicketDto(
                        displayTicketNumber(queueType, ticket.number()),
                        null,
                        ticket.orderId(),
                        ticket.estimatedWaitMinutes(),
                        null
                ))
                .toList();

        return new PublicQueueDisplayResponse(
                queueType,
                queueLabel(queueType),
                nowServing,
                nowServingLines,
                nextInLine,
                status.stats().totalWaiting(),
                status.stats().averageWaitTime(),
                instruction(queueType),
                status.lastUpdated()
        );
    }

    private String displayTicketNumber(QueueType queueType, String ticketNumber) {
        String prefix = queueType == QueueType.PRODUCTION ? "P" : "A";
        if (ticketNumber == null || ticketNumber.isBlank()) {
            return prefix + "---";
        }

        try {
            return prefix + String.format("%03d", Integer.parseInt(ticketNumber));
        } catch (NumberFormatException ex) {
            return prefix + ticketNumber;
        }
    }

    private String queueLabel(QueueType queueType) {
        return queueType == QueueType.PRODUCTION ? "Production Queue" : "Accounting Queue";
    }

    private String instruction(QueueType queueType) {
        return queueType == QueueType.PRODUCTION
                ? "Please proceed to the production area"
                : "Please proceed to the cashier";
    }

    public List<QueueTicketResponse> getTicketsByQueueType(QueueType queueType, LocalDate date) {
        return queueTicketRepo
                .findAllByQueueTypeAndQueueDate(
                        queueType,
                        date)
                .stream()
                .map(QueueTicketMapper::mapToResponse)
                .toList();
    }

    public Integer getTicketNumberByOrderIdAndQueueType(Long orderId, String queueTypeStr) {
        QueueType queueType = Arrays.stream(QueueType.values())
                .filter(q -> q.name().equalsIgnoreCase(queueTypeStr)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Queue Type found with name " + queueTypeStr));
        return queueTicketRepo.findByQueueTypeAndOrderId(queueType, orderId)
                .map(QueueTicket::getTicketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket number not found for orderId: " + orderId));
    }

    @Transactional
    public QueueTicket updateTicketStatus(
            QueueType queueType,
            Long ticketId,
            Long orderId,
            TicketStatus newStatus,
            String productionLine) {

        QueueTicket ticket;
        if (ticketId != null) {
            ticket = queueTicketRepo.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
        } else if (orderId != null) {
            ticket = queueTicketRepo.findByQueueTypeAndOrderId(queueType, orderId)
                    .orElseThrow(() -> new TicketNotFoundException(
                            "No Ticket found in " + queueType + " queue for order id: " + orderId));
        } else {
            throw new IllegalArgumentException("At least one of Order id or Ticket id must be provided");
        }

        TicketStatus currentStatus = ticket.getTicketStatus();

        // ✅ Optional: enforce valid transitions
        validateTransition(currentStatus, newStatus);

        String normalizedProductionLine = normalizeProductionLine(productionLine);
        if (newStatus == TicketStatus.SERVING && queueType == QueueType.PRODUCTION) {
            if (normalizedProductionLine == null) {
                throw new InvalidTicketStateException("Production line is required for production tickets");
            }

            if (currentStatus == TicketStatus.WAITING) {
                enforceFirstWaitingTicket(queueType, ticket);
            }

            ticket.setProductionLine(normalizedProductionLine);
        }

        ticket.setTicketStatus(newStatus);

        // ✅ handle timestamps based on state
        if (newStatus == TicketStatus.SERVING) {
            ticket.setCalledAt(LocalDateTime.now());
        }

        if (newStatus == TicketStatus.COMPLETED
                || newStatus == TicketStatus.CANCELLED) {
            ticket.setCompletedAt(LocalDateTime.now());
        }

        QueueTicket saved = queueTicketRepo.save(ticket);

        // ✅ publish event
        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(ticket.getQueueType()));

        return saved;
    }

    private void enforceFirstWaitingTicket(QueueType queueType, QueueTicket ticket) {
        QueueTicket nextWaitingTicket = queueTicketRepo.findNextWaiting(queueType)
                .orElseThrow(() -> new InvalidTicketStateException("No waiting production tickets"));

        if (!nextWaitingTicket.getId().equals(ticket.getId())) {
            throw new InvalidTicketStateException(
                    "Ticket " + ticket.getTicketNumber() + " cannot be served before ticket "
                            + nextWaitingTicket.getTicketNumber());
        }
    }

    private String normalizeProductionLine(String productionLine) {
        if (productionLine == null || productionLine.isBlank()) {
            return null;
        }

        String normalized = productionLine.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("A") && !normalized.equals("B")) {
            throw new InvalidTicketStateException("Production line must be A or B");
        }

        return normalized;
    }

    private int lineOrder(String productionLine) {
        if ("A".equalsIgnoreCase(productionLine)) {
            return 0;
        }

        if ("B".equalsIgnoreCase(productionLine)) {
            return 1;
        }

        return 2;
    }

    private int ticketNumberOrder(String ticketNumber) {
        if (ticketNumber == null || ticketNumber.isBlank()) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(ticketNumber);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private void validateTransition(
            TicketStatus current,
            TicketStatus next) {

        if (current == next)
            return;

        switch (current) {

            case WAITING -> {
                if (next != TicketStatus.SERVING
                        && next != TicketStatus.CANCELLED) {
                    throw new InvalidTicketStateException(
                            "WAITING can only go to SERVING or CANCELLED");
                }
            }

            case SERVING -> {
                if (next != TicketStatus.COMPLETED
                        && next != TicketStatus.CANCELLED) {
                    throw new InvalidTicketStateException(
                            "SERVING can only go to COMPLETED or CANCELLED");
                }
            }

            case COMPLETED, CANCELLED -> {
                throw new InvalidTicketStateException(
                        "Final state cannot be changed");
            }
        }
    }

    @Transactional
    public QueueTicket returnToQueue(Long ticketId) {

        QueueTicket ticket = queueTicketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getTicketStatus() != TicketStatus.MISSED) {
            throw new RuntimeException("Only MISSED tickets can return");
        }

        if (ticket.getMissedAt() == null) {
            throw new RuntimeException("Missed time not found");
        }

        long minutes = Duration
                .between(ticket.getMissedAt(), LocalDateTime.now())
                .toMinutes();

        // ✅ رجع خلال 10 دقائق
        if (minutes <= 10) {

            ticket.setTicketStatus(TicketStatus.WAITING);

            ticket.setMissedAt(null);
            ticket.setUserId(null);
            ticket.setCalledAt(null);
            ticket.setCompletedAt(null);

            return queueTicketRepo.save(ticket);
        }

        // ❌ تأخر → خلاص انتهت
        else {

            ticket.setTicketStatus(TicketStatus.CANCELLED);
            ticket.setCompletedAt(LocalDateTime.now());

            return queueTicketRepo.save(ticket);
        }
    }

@Transactional
public QueueTicket recallMissedTicket(QueueType queueType) {

    if (queueType != QueueType.ACCOUNTING) {
        throw new AccessDeniedException("Recall is only allowed for ACCOUNTING queue");
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) auth.getPrincipal();
    Long userId = jwt.getClaim("userId");

    if (userId == null) {
        throw new AccessDeniedException("userId missing in token");
    }

    if (!tellerSessionService.isActive(userId, queueType)) {
        throw new AccessDeniedException("Teller not logged in");
    }

    Optional<QueueTicket> current =
            queueTicketRepo.findByUserIdAndTicketStatusAndQueueDate(
                    userId,
                    TicketStatus.SERVING,
                    LocalDate.now());

    if (current.isPresent()) {
        throw new RuntimeException("Finish current ticket first");
    }

    QueueTicket ticket = queueTicketRepo
            .findFirstByQueueTypeAndTicketStatusOrderByTicketNumberAsc(
                    queueType,
                    TicketStatus.MISSED)
            .orElseThrow(() -> new RuntimeException("No MISSED tickets"));

    if (ticket.getMissedAt() == null) {
        throw new RuntimeException("Invalid ticket");
    }

    long minutes = Duration
            .between(ticket.getMissedAt(), LocalDateTime.now())
            .toMinutes();

    if (minutes > 10) {

        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setCompletedAt(LocalDateTime.now());
        queueTicketRepo.save(ticket);

        appEventPublisher.publishEvent(new QueueUpdatedEvent(queueType)); 

        throw new RuntimeException("Ticket expired (more than 10 minutes)");
    }

    ticket.setTicketStatus(TicketStatus.SERVING);
    ticket.setCalledAt(LocalDateTime.now());
    ticket.setUserId(userId);
    ticket.setMissedAt(null);

    QueueTicket saved = queueTicketRepo.save(ticket);

    appEventPublisher.publishEvent(new QueueUpdatedEvent(queueType));

    return saved;
}
    @Transactional
    public QueueTicket recallCurrent(QueueType queueType) {

        if (queueType != QueueType.ACCOUNTING) {
            throw new RuntimeException("Recall is only allowed for ACCOUNTING queue");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        Long userId = jwt.getClaim("userId");

        if (userId == null) {
            throw new AccessDeniedException("userId missing in token");
        }

        if (!tellerSessionService.isActive(userId, queueType)) {
            throw new AccessDeniedException("Teller not logged in");
        }

        // 🔍 نجيب التذكرة الحالية (SERVING)
        QueueTicket ticket = queueTicketRepo
                .findByUserIdAndTicketStatusAndQueueDate(
                        userId,
                        TicketStatus.SERVING,
                        LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No current ticket"));

        // 🔥 فقط نحدث وقت النداء
        ticket.setCalledAt(LocalDateTime.now());

        QueueTicket saved = queueTicketRepo.save(ticket);

        // 🔔 realtime update
        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return saved;
    }
}

package com.project.queue_service.service;

import com.project.queue_service.client.OrderClient;
import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueUpdatedEvent;
import com.project.queue_service.exception.*;
import com.project.queue_service.model.*;
import com.project.queue_service.repository.QueueTicketRepo;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueManager {

    private final OrderClient orderClient;
    private final QueueTicketRepo queueTicketRepo;
    private final QueueCounterService queueCounterService;
    private final ApplicationEventPublisher appEventPublisher;

    @Transactional
    public QueueTicket issueTicket(QueueType queueType) {

        int nextNumber =
                queueCounterService.getAndIncrementTicketNumber(queueType);

        QueueTicket ticket = new QueueTicket();
        ticket.setQueueType(queueType);
        ticket.setQueueDate(LocalDate.now());
        ticket.setTicketNumber(nextNumber);
        ticket.setTicketStatus(TicketStatus.WAITING);
        ticket.setCreatedAt(LocalDateTime.now());

        QueueTicket saved = queueTicketRepo.save(ticket);

        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return saved;
    }

    @Transactional
    public QueueTicket addToProduction(Long orderId, Long orderItemId) {

        try {

            var item = orderClient.getOrderItemById(orderId, orderItemId);

            if (!"SERVICE".equalsIgnoreCase(item.getProductType())) {
                throw new InvalidTicketStateException(
                        "Only SERVICE items can enter production");
            }

            if (!"PAID".equalsIgnoreCase(item.getStatus())) {
                throw new InvalidTicketStateException(
                        "Only PAID items can enter production");
            }

            if (queueTicketRepo.existsByOrderItemIdAndQueueType(
                    orderItemId,
                    QueueType.PRODUCTION)) {

                throw new InvalidTicketStateException(
                        "Item already exists in production queue");
            }

            int nextNumber =
                    queueCounterService.getAndIncrementTicketNumber(
                            QueueType.PRODUCTION);

            QueueTicket ticket = new QueueTicket();
            ticket.setQueueType(QueueType.PRODUCTION);
            ticket.setQueueDate(LocalDate.now());
            ticket.setTicketNumber(nextNumber);
            ticket.setTicketStatus(TicketStatus.WAITING);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setOrderId(orderId);
            ticket.setOrderItemId(orderItemId);

            QueueTicket saved = queueTicketRepo.save(ticket);

            orderClient.updateOrderItemStatus(
                    orderItemId,
                    Map.of("status", "IN_PRODUCTION"));

            appEventPublisher.publishEvent(
                    new QueueUpdatedEvent(QueueType.PRODUCTION));

            return saved;

        } catch (FeignException ex) {
            log.error("Feign communication error", ex);
            throw new ExternalServiceException(
                    "Order service communication error");
        }
    }

    @Transactional(noRollbackFor = NoWaitingTicketException.class)
    public QueueTicket advanceQueue(
            QueueType queueType,
            TellerAction action) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Invalid authentication");
        }

        Long tellerId = jwt.getClaim("employeeId");

        queueTicketRepo
                .findByTellerIdAndTicketStatus(
                        tellerId,
                        TicketStatus.SERVING)
                .ifPresent(t -> {
                    t.setTicketStatus(
                            action == TellerAction.NEXT ?
                                    TicketStatus.COMPLETED :
                                    TicketStatus.CANCELLED
                    );
                    t.setCompletedAt(LocalDateTime.now());
                    queueTicketRepo.save(t);
                });

        QueueTicket nextTicket = queueTicketRepo
                .findNextWaiting(queueType)
                .map(t -> {

                    t.setTellerId(tellerId);
                    t.setCalledAt(LocalDateTime.now());
                    t.setTicketStatus(TicketStatus.SERVING);

                    QueueTicket saved =
                            queueTicketRepo.save(t);

                    if (queueType == QueueType.PRODUCTION
                            && saved.getOrderItemId() != null) {

                        var item = orderClient.getOrderItemById(
                                saved.getOrderId(),
                                saved.getOrderItemId());

                        if ("IN_PRODUCTION".equalsIgnoreCase(item.getStatus())) {

                            orderClient.updateOrderItemStatus(
                                    saved.getOrderItemId(),
                                    Map.of("status", "IN_PROGRESS"));
                        }
                    }

                    return saved;
                })
                .orElseThrow(() ->
                        new NoWaitingTicketException(queueType));

        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return nextTicket;
    }

    @Transactional
    public QueueTicket completeTicket(
            QueueType queueType,
            Long ticketId) {

        QueueTicket ticket =
                queueTicketRepo.findById(ticketId)
                        .orElseThrow(() ->
                                new TicketNotFoundException(ticketId));

        if (ticket.getTicketStatus() != TicketStatus.SERVING) {
            throw new InvalidTicketStateException(
                    "Only SERVING ticket can be completed");
        }

        ticket.setTicketStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());

        QueueTicket saved = queueTicketRepo.save(ticket);

        if (queueType == QueueType.PRODUCTION
                && saved.getOrderItemId() != null) {

            orderClient.updateOrderItemStatus(
                    saved.getOrderItemId(),
                    Map.of("status", "READY_FOR_PICKUP"));
        }

        appEventPublisher.publishEvent(
                new QueueUpdatedEvent(queueType));

        return saved;
    }

    public QueueResponseDto getQueueStatus(
            QueueType queueType) {

        List<QueueTicket> tickets =
                queueTicketRepo
                        .findAllByQueueTypeAndQueueDateAndTicketStatusIn(
                                queueType,
                                LocalDate.now(),
                                List.of(
                                        TicketStatus.SERVING,
                                        TicketStatus.WAITING));

        return QueueDtoUtil.buildQueueResponse(tickets);
    }
}
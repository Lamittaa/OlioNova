package com.project.queue_service.service;

import com.project.queue_service.exception.NoWaitingTicketException;
import com.project.queue_service.client.OrderClient;
import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueUpdatedEvent;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TellerAction;
import com.project.queue_service.model.TicketStatus;
import com.project.queue_service.repository.QueueTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueManager {


private final OrderClient orderClient;


    private final QueueTicketRepo queueTicketRepo;
    private final QueueCounterService queueCounterService;
    private final ApplicationEventPublisher appEventPublisher;

    @Transactional
    public QueueTicket issueTicket(String queueType) {

        int nextNumber = queueCounterService.getAndIncrementTicketNumber(queueType);

        QueueTicket newTicket = new QueueTicket();
        newTicket.setTicketNumber(nextNumber);
        newTicket.setTicketStatus(TicketStatus.WAITING);
        newTicket.setQueueType(queueType);
        newTicket.setQueueDate(LocalDate.now());
        newTicket.setCreatedAt(LocalDateTime.now());

        QueueTicket saved = queueTicketRepo.save(newTicket);

        appEventPublisher.publishEvent(new QueueUpdatedEvent(queueType));

        return saved;
    }

    @Transactional(noRollbackFor = NoWaitingTicketException.class)
    public QueueTicket advanceQueue(
            String queueType,
            Long tellerId,
            TellerAction action
    ) {

        // Close current serving ticket for this teller
        queueTicketRepo
                .findByTellerIdAndTicketStatus(
                        tellerId,
                        TicketStatus.SERVING
                )
                .ifPresent(t -> {
                    if (action == TellerAction.NEXT) {
                        t.setTicketStatus(TicketStatus.COMPLETED);
                    } else {
                        t.setTicketStatus(TicketStatus.CANCELLED);
                    }
                    t.setCompletedAt(LocalDateTime.now());
                    queueTicketRepo.save(t);
                });

        // Call next waiting ticket
        QueueTicket nextTicket = queueTicketRepo
                .findNextWaiting(queueType)
                .map(t -> {
                    t.setTellerId(tellerId);
                    t.setCalledAt(LocalDateTime.now());
                    t.setTicketStatus(TicketStatus.SERVING);
                    return queueTicketRepo.save(t);
                })
                .orElseThrow(() ->
                        new NoWaitingTicketException("No tickets in queue")
                );

        appEventPublisher.publishEvent(new QueueUpdatedEvent(queueType));

        return nextTicket;
    }

    public QueueResponseDto getQueueStatus(String queueType) {

        List<QueueTicket> tickets =
                queueTicketRepo.findAllByQueueTypeAndQueueDateAndTicketStatusIn(
                        queueType,
                        LocalDate.now(),
                        List.of(TicketStatus.SERVING, TicketStatus.WAITING)
                );

        return QueueDtoUtil.buildQueueResponse(tickets);
    }

@Transactional
public QueueTicket issueProductionTicket(Long orderId) {

    int nextNumber = queueCounterService
            .getAndIncrementTicketNumber("PRODUCTION");

    QueueTicket newTicket = new QueueTicket();
    newTicket.setTicketNumber(nextNumber);
    newTicket.setTicketStatus(TicketStatus.WAITING);
    newTicket.setQueueType("PRODUCTION");
    newTicket.setQueueDate(LocalDate.now());
    newTicket.setCreatedAt(LocalDateTime.now());
    newTicket.setOrderId(orderId);

    QueueTicket saved = queueTicketRepo.save(newTicket);

    appEventPublisher.publishEvent(
            new QueueUpdatedEvent("PRODUCTION")
    );

    return saved;
}
@Transactional
public QueueTicket addToProduction(Long orderId) {

    int nextNumber = queueCounterService.getAndIncrementTicketNumber("PRODUCTION");

    QueueTicket ticket = new QueueTicket();
    ticket.setQueueType("PRODUCTION");
    ticket.setQueueDate(LocalDate.now());
    ticket.setTicketNumber(nextNumber);
    ticket.setTicketStatus(TicketStatus.WAITING);
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setOrderId(orderId);

    QueueTicket saved = queueTicketRepo.save(ticket);

    appEventPublisher.publishEvent(new QueueUpdatedEvent("PRODUCTION"));

    return saved;
}
@Transactional
public QueueTicket completeTicket(String queueType, Long ticketId) {

    QueueTicket ticket = queueTicketRepo.findById(ticketId)
            .orElseThrow(() ->
                    new RuntimeException("Ticket not found"));

    if (!ticket.getQueueType().equalsIgnoreCase(queueType)) {
        throw new RuntimeException("Queue type mismatch");
    }

    if (ticket.getTicketStatus() != TicketStatus.SERVING) {
        throw new RuntimeException("Only SERVING ticket can be completed");
    }

    ticket.setTicketStatus(TicketStatus.COMPLETED);
    ticket.setCompletedAt(LocalDateTime.now());

    queueTicketRepo.save(ticket);

    // 🔥 لو Production حدث الأوردر
    if ("PRODUCTION".equalsIgnoreCase(queueType)
            && ticket.getOrderId() != null) {

        orderClient.updateOrderStatus(
                ticket.getOrderId(),
                Map.of("status", "COMPLETED")
        );
    }

    return ticket;
}

}

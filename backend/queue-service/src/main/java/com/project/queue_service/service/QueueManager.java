package com.project.queue_service.service;


import com.project.queue_service.client.OrderClient;
import com.project.queue_service.dto.OrderResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class QueueManager {

    
    private final QueueTicketRepo queueTicketRepo;
    private final QueueCounterService queueCounterService;
    private final ApplicationEventPublisher appEventPublisher;
    private final OrderClient orderClient;
 @Transactional
public QueueTicket issueTicket(QueueType queueType, Long orderId) {

    // ✅ منع إنشاء تذكرة مكررة لنفس الطلب
  if (queueTicketRepo.existsByOrderIdAndQueueType(orderId, queueType)) {

    throw new DuplicateTicketException(
            "Ticket already exists for this order in " + queueType
    );
}

    int nextNumber =
            queueCounterService.getAndIncrementTicketNumber(queueType);

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

    Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
        throw new AccessDeniedException("Invalid authentication");
    }

    Long userId = jwt.getClaim("userId");

    if (userId == null) {
        throw new AccessDeniedException("userId missing in token");
    }

    // 1️⃣ check current serving ticket
    Optional<QueueTicket> servingTicket =
            queueTicketRepo.findByUserIdAndTicketStatusAndQueueDate(
                    userId,
                    TicketStatus.SERVING,
                    LocalDate.now()
            );

    servingTicket.ifPresent(t -> postServingTicket(t, action));

    // 2️⃣ get next waiting ticket
    Optional<QueueTicket> next =
            queueTicketRepo.findNextWaiting(queueType);

    if (next.isEmpty()) {

        // إذا أنهينا تذكرة فقط لا نحتاج فتح أخرى
        if (servingTicket.isPresent()) {
            appEventPublisher.publishEvent(
                    new QueueUpdatedEvent(queueType)
            );
        }

        throw new NoWaitingTicketException(queueType);
    }

    QueueTicket t = next.get();

    t.setUserId(userId);
    t.setCalledAt(LocalDateTime.now());
    t.setTicketStatus(TicketStatus.SERVING);

    QueueTicket saved = queueTicketRepo.save(t);

    appEventPublisher.publishEvent(
            new QueueUpdatedEvent(queueType)
    );

    return saved;
}

private QueueTicket postServingTicket(
        QueueTicket t,
        TellerAction action) {

    TicketStatus status;

    if (action == TellerAction.NEXT) {

        status = TicketStatus.COMPLETED;

    } else {

        status = TicketStatus.CANCELLED;
    }

    t.setTicketStatus(status);
    t.setCompletedAt(LocalDateTime.now());

    return queueTicketRepo.save(t);
}

//     @Transactional
//     public QueueTicket completeTicket(
//             QueueType queueType,
//             Long ticketId) {

//         QueueTicket ticket =
//                 queueTicketRepo.findById(ticketId)
//                         .orElseThrow(() ->
//                                 new TicketNotFoundException(ticketId));

//         if (ticket.getTicketStatus() != TicketStatus.SERVING) {
//             throw new InvalidTicketStateException(
//                     "Only SERVING ticket can be completed");
//         }

//         ticket.setTicketStatus(TicketStatus.COMPLETED);
//         ticket.setCompletedAt(LocalDateTime.now());

//         QueueTicket saved = queueTicketRepo.save(ticket);

//         if (queueType == QueueType.PRODUCTION
//                 && saved.getOrderItemId() != null) {

//             orderClient.updateOrderItemStatus(
//                     saved.getOrderItemId(),
//                     Map.of("status", "READY_FOR_PICKUP"));
//         }

//         appEventPublisher.publishEvent(
//                 new QueueUpdatedEvent(queueType));

//         return saved;
//     }

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
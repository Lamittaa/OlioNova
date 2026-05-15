package com.project.queue_service.service;

import com.project.queue_service.dto.QueueUpdatedEvent;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TicketStatus;
import com.project.queue_service.repository.QueueTicketRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueueTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueTimeoutScheduler.class);

    private final QueueTicketRepo queueTicketRepo;
    private final ApplicationEventPublisher appEventPublisher;

    public QueueTimeoutScheduler(QueueTicketRepo queueTicketRepo, ApplicationEventPublisher appEventPublisher) {
        this.queueTicketRepo = queueTicketRepo;
        this.appEventPublisher = appEventPublisher;
    }
    // ⏱️ يشتغل كل دقيقة
    @Scheduled(fixedRate = 60000)
    public void checkTimeouts() {

        List<QueueTicket> tickets = queueTicketRepo
                .findAllByTicketStatusAndQueueDate(
                        TicketStatus.SERVING,
                        LocalDate.now()
                );

        for (QueueTicket ticket : tickets) {

            if (ticket.getCalledAt() == null) continue;

            long minutes = Duration
                    .between(ticket.getCalledAt(), LocalDateTime.now())
                    .toMinutes();

            if (minutes >= 5) {

                ticket.setTicketStatus(TicketStatus.MISSED);
                ticket.setMissedAt(LocalDateTime.now());

                queueTicketRepo.save(ticket);
   appEventPublisher.publishEvent(
                        new QueueUpdatedEvent(ticket.getQueueType())
                );

                log.info("Ticket {} moved to MISSED بسبب timeout", ticket.getTicketNumber());
            }
        }
    }
}

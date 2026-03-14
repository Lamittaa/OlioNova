package com.project.queue_service.service;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.dto.QueueUpdatedEvent;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TicketStatus;
import com.project.queue_service.repository.QueueTicketRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueUpdateListener {

        private final QueueTicketRepo queueTicketRepo;
        private final RealtimeNotifier realtimeNotifier;

        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleQueueUpdated(QueueUpdatedEvent event) {

                var statuses = List.of(TicketStatus.SERVING, TicketStatus.WAITING);

                List<QueueTicket> tickets = queueTicketRepo.findAllByQueueTypeAndQueueDateAndTicketStatusIn(
                                event.getQueueType(),
                                LocalDate.now(),
                                statuses);

                QueueResponseDto responseDto = QueueDtoUtil.buildQueueResponse(tickets);

                realtimeNotifier.publishUpdate(event.getQueueType(), responseDto);
        }
}

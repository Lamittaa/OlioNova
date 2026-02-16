package com.project.queue_service.repository;


import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TicketStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTicketRepo extends JpaRepository<QueueTicket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findFirstByQueueTypeAndTicketStatusOrderByTicketNumberAsc(
            String queueType,
            TicketStatus ticketStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findByTellerIdAndTicketStatus(
            Long tellerId,
            TicketStatus ticketStatus
    );

    List<QueueTicket> findAllByQueueTypeAndQueueDateAndTicketStatusIn(
            String queueType,
            LocalDate queueDate,
            List<TicketStatus> ticketStatusList
    );

    default Optional<QueueTicket> findNextWaiting(String queueType) {
        return findFirstByQueueTypeAndTicketStatusOrderByTicketNumberAsc(
                queueType,
                TicketStatus.WAITING
        );
    }
}

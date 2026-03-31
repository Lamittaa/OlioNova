package com.project.queue_service.repository;

import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.QueueType;
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
            QueueType queueType,
            TicketStatus ticketStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueTicket> findByUserIdAndTicketStatusAndQueueDate(
            Long userId,
            TicketStatus ticketStatus,
            LocalDate queueDate
    );

    List<QueueTicket> findAllByQueueTypeAndQueueDateAndTicketStatusIn(
            QueueType queueType,
            LocalDate queueDate,
            List<TicketStatus> ticketStatusList
    );

    default Optional<QueueTicket> findNextWaiting(QueueType queueType) {
        return findFirstByQueueTypeAndTicketStatusOrderByTicketNumberAsc(
                queueType,
                TicketStatus.WAITING
        );
    }


    boolean existsByOrderIdAndQueueType(
            Long orderId,
            QueueType queueType
    );

    List<QueueTicket> findAllByQueueTypeAndQueueDate(QueueType queueType, LocalDate date);

    Optional<QueueTicket> findByQueueTypeAndOrderId(QueueType queueType, Long orderId);

    List<QueueTicket> findAllByTicketStatusAndQueueDate(
        TicketStatus status,
        LocalDate date
);
}
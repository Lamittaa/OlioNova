package com.project.queue_service.service;

import com.project.queue_service.dto.*;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TicketStatus;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public class QueueDtoUtil {

    public static QueueResponseDto buildQueueResponse(List<QueueTicket> tickets) {

        // ================= SERVING =================
        List<ServingTicketDto> serving = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.SERVING)
                .map(t -> new ServingTicketDto(
                        new TicketDto(
                                t.getId().toString(),
                                t.getTicketNumber().toString(),
                                t.getQueueType(),
                                t.getOrderId(),
                                t.getCreatedAt().toString()
                        ),
                        t.getQueueType(),
                        t.getCalledAt() != null
                                ? t.getCalledAt().toString()
                                : null
                ))
                .toList();

        // ================= WAITING =================
        List<WaitingTicketDto> waiting = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.WAITING)
                .sorted(Comparator.comparing(QueueTicket::getCreatedAt))
                .map(t -> new WaitingTicketDto(
                        t.getId().toString(),
                        t.getTicketNumber().toString(),
                        t.getQueueType(),
                        t.getOrderId(),
                        t.getCreatedAt().toString(),
                        estimateWaitMinutes(tickets, t)
                ))
                .toList();

        // ================= STATS =================
        int totalWaiting = waiting.size();

        int averageWaitTime = calculateAverageWaitTime(tickets);

        int activeCounters = (int) tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.SERVING)
                .map(QueueTicket::getUserId)
                .distinct()
                .count();

        return new QueueResponseDto(
                serving,
                waiting,
                new QueueStatsDto(totalWaiting, averageWaitTime, activeCounters),
                java.time.Instant.now().toString()
        );
    }

    // =========================================================
    // WAIT TIME ESTIMATION
    // =========================================================
    private static int estimateWaitMinutes(List<QueueTicket> allTickets, QueueTicket ticket) {

        List<QueueTicket> waitingTickets = allTickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.WAITING)
                .sorted(Comparator.comparing(QueueTicket::getCreatedAt))
                .toList();

        int position = waitingTickets.indexOf(ticket);

        // مثال: كل طلب = 5 دقائق
        return position * 5;
    }

    // =========================================================
    // AVERAGE WAIT TIME (SERVING ONLY)
    // =========================================================
    private static int calculateAverageWaitTime(List<QueueTicket> tickets) {

        List<Long> waitTimes = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.SERVING)
                .filter(t -> t.getCalledAt() != null)
                .map(t -> Duration.between(
                        t.getCreatedAt(),
                        t.getCalledAt()
                ).toMinutes())
                .toList();

        if (waitTimes.isEmpty()) {
            return 0;
        }

        return (int) waitTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
    }
}

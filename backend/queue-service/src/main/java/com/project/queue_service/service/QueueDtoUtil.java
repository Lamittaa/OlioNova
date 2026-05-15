package com.project.queue_service.service;

import com.project.queue_service.dto.*;
import com.project.queue_service.model.QueueTicket;
import com.project.queue_service.model.TicketStatus;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class QueueDtoUtil {

    public static QueueResponseDto buildQueueResponse(List<QueueTicket> tickets) {

        List<ServingTicketDto> serving = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.SERVING)
                .map(t -> new ServingTicketDto(
                        new TicketDto(
                                safeString(t.getId()),
                                safeString(t.getTicketNumber()),
                                t.getQueueType(),
                                t.getOrderId(),
                                safeString(t.getCreatedAt()),
                                t.getProductionLine()
                        ),
                        t.getQueueType(),
                        t.getCalledAt() != null
                                ? t.getCalledAt().toString()
                                : null
                ))
                .toList();

        List<WaitingTicketDto> waiting = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.WAITING)
                .sorted(Comparator.comparing(
                        QueueTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new WaitingTicketDto(
                        safeString(t.getId()),
                        safeString(t.getTicketNumber()),
                        t.getQueueType(),
                        t.getOrderId(),
                        safeString(t.getCreatedAt()),
                        estimateWaitMinutes(tickets, t)
                ))
                .toList();

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


    private static int estimateWaitMinutes(List<QueueTicket> allTickets, QueueTicket ticket) {

        List<QueueTicket> waitingTickets = allTickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.WAITING)
                .sorted(Comparator.comparing(
                        QueueTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int position = waitingTickets.indexOf(ticket);

        return position * 5;
    }

 
    private static int calculateAverageWaitTime(List<QueueTicket> tickets) {

        List<Long> waitTimes = tickets.stream()
                .filter(t -> t.getTicketStatus() == TicketStatus.SERVING)
                .filter(t -> t.getCalledAt() != null)
                .filter(t -> t.getCreatedAt() != null)
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

    private static String safeString(Object value) {
        return Objects.toString(value, "");
    }
}

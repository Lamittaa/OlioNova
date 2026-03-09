package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

public record TicketDto(
       String id,
        String number,
        QueueType type,
        Long orderId,
        String checkInTime
) { }

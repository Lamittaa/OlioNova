package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

// Represents a waiting ticket
public record WaitingTicketDto(
         String id,
        String number,
       QueueType type,
        Long orderId,
        String checkInTime,
        int estimatedWaitMinutes
) { }

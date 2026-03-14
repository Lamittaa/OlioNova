package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

public record WaitingTicketDto(
         String id,
        String number,
       QueueType type,
        Long orderId,
        String checkInTime,
        int estimatedWaitMinutes
) { }

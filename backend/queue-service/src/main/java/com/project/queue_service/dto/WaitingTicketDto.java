package com.project.queue_service.dto;


// Represents a waiting ticket
public record WaitingTicketDto(
         String id,
        String number,
        String type,
        Long orderId,
        String checkInTime,
        int estimatedWaitMinutes
) { }

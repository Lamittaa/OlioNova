package com.project.queue_service.dto;
public record TicketDto(
       String id,
        String number,
        String type,
        Long orderId,
        String checkInTime
) { }

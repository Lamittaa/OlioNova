package com.project.queue_service.dto;

public record PublicQueueTicketDto(
        String number,
        String calledAt,
        Long orderId,
        Integer estimatedWaitMinutes,
        String productionLine
) {
}

package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

import java.util.List;

public record PublicQueueDisplayResponse(
        QueueType queueType,
        String queueLabel,
        PublicQueueTicketDto nowServing,
        List<PublicQueueTicketDto> nowServingLines,
        List<PublicQueueTicketDto> nextInLine,
        int totalWaiting,
        int averageWaitMinutes,
        String instruction,
        String lastUpdated
) {
}

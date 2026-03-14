package com.project.queue_service.dto;

import java.util.List;

public record QueueResponseDto(
        List<ServingTicketDto> serving,
        List<WaitingTicketDto> waiting,
        QueueStatsDto stats,
        String lastUpdated 
) { }
package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

public record ServingTicketDto(
        TicketDto ticket,
      QueueType counter,
        String startedAt 
) { }
package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

// Represents a ticket currently being served
public record ServingTicketDto(
        TicketDto ticket,
      QueueType counter,
        String startedAt // ISO-8601 string
) { }
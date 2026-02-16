package com.project.queue_service.dto;

// Represents a ticket currently being served
public record ServingTicketDto(
        TicketDto ticket,
        String counter,
        String startedAt // ISO-8601 string
) { }
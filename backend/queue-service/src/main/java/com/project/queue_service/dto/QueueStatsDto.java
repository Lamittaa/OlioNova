package com.project.queue_service.dto;

public record QueueStatsDto(
        int totalWaiting,
        int averageWaitTime,
        int activeCounters
) { }

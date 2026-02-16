package com.project.queue_service.dto;

import lombok.Getter;

@Getter
public class QueueUpdatedEvent {
    private final String queueType;

    public QueueUpdatedEvent(String queueType) {
        this.queueType = queueType;
    }
    
}

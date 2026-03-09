package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

import lombok.Getter;

@Getter
public class QueueUpdatedEvent {
    private final QueueType queueType;

    public QueueUpdatedEvent(QueueType queueType) {
        this.queueType = queueType;
    }
    
}

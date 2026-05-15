package com.project.queue_service.dto;

import com.project.queue_service.model.QueueType;

public class QueueUpdatedEvent {
    private final QueueType queueType;

    public QueueUpdatedEvent(QueueType queueType) {
        this.queueType = queueType;
    }

    public QueueType getQueueType() {
        return queueType;
    }
}

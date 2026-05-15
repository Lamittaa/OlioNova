package com.project.queue_service.service;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.model.QueueType;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class RealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public void publishUpdate(QueueType queueType, QueueResponseDto queueResponseDto) {

        messagingTemplate.convertAndSend(
                "/topic/queue/" + queueType,
                queueResponseDto
        );
    }
}

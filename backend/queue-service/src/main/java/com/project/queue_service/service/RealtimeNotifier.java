package com.project.queue_service.service;

import com.project.queue_service.dto.QueueResponseDto;
import com.project.queue_service.model.QueueTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void publishUpdate(String queueType, QueueResponseDto queueResponseDto) {

        messagingTemplate.convertAndSend(
                "/topic/queue/" + queueType,
                queueResponseDto
        );
    }
}

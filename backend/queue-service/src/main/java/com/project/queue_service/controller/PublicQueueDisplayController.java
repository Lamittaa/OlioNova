package com.project.queue_service.controller;

import com.project.queue_service.dto.PublicQueueDisplayResponse;
import com.project.queue_service.model.QueueType;
import com.project.queue_service.service.QueueManager;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/queues")
public class PublicQueueDisplayController {

    private final QueueManager queueManager;

    public PublicQueueDisplayController(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @GetMapping("/{queueType}/display")
    public ResponseEntity<PublicQueueDisplayResponse> getDisplay(@PathVariable QueueType queueType) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(queueManager.getPublicQueueDisplay(queueType));
    }
}

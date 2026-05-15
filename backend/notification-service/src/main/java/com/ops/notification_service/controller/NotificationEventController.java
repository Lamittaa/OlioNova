package com.ops.notification_service.controller;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/events")
public class NotificationEventController {

    private final ApplicationEventPublisher eventPublisher;
    private final String internalApiKey;

    public NotificationEventController(
            ApplicationEventPublisher eventPublisher,
            @Value("${security.api-key.internal:}") String internalApiKey
    ) {
        this.eventPublisher = eventPublisher;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping("/order-ready-for-pickup")
    public ResponseEntity<Void> publishOrderReadyForPickup(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody OrderReadyForPickupEvent event
    ) {
        if (!internalRequestAllowed(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        eventPublisher.publishEvent(event);
        return ResponseEntity.accepted().build();
    }

    private boolean internalRequestAllowed(String apiKey) {
        return internalApiKey != null && !internalApiKey.isBlank() && internalApiKey.equals(apiKey);
    }
}

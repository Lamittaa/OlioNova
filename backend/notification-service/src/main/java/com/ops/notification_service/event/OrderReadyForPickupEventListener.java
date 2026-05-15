package com.ops.notification_service.event;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import com.ops.notification_service.service.SmsNotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderReadyForPickupEventListener {

    private final SmsNotificationService smsNotificationService;

    public OrderReadyForPickupEventListener(SmsNotificationService smsNotificationService) {
        this.smsNotificationService = smsNotificationService;
    }

    @Async
    @EventListener
    public void onOrderReadyForPickup(OrderReadyForPickupEvent event) {
        smsNotificationService.handleOrderReadyForPickup(event);
    }
}

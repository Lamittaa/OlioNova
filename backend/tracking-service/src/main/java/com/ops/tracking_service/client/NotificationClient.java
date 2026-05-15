package com.ops.tracking_service.client;

import com.ops.tracking_service.config.FeignAuthForwardConfig;
import com.ops.tracking_service.dto.OrderReadyForPickupEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", configuration = FeignAuthForwardConfig.class)
public interface NotificationClient {

    @PostMapping("/api/notifications/events/order-ready-for-pickup")
    void publishOrderReadyForPickup(@RequestBody OrderReadyForPickupEvent event);
}

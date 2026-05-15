package com.ops.tracking_service.service;

import com.ops.tracking_service.client.CustomerClient;
import com.ops.tracking_service.client.NotificationClient;
import com.ops.tracking_service.client.OrderClient;
import com.ops.tracking_service.client.ProductionBatchClient;
import com.ops.tracking_service.dto.CustomerResponse;
import com.ops.tracking_service.dto.OrderReadyForPickupEvent;
import com.ops.tracking_service.dto.OrderResponse;
import com.ops.tracking_service.dto.ProductionBatchResponse;
import com.ops.tracking_service.model.TankCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class OrderReadyForPickupEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderReadyForPickupEventPublisher.class);
    private static final String READY_FOR_PICKUP = "READY_FOR_PICKUP";

    private final ProductionBatchClient productionBatchClient;
    private final OrderClient orderClient;
    private final CustomerClient customerClient;
    private final NotificationClient notificationClient;

    public OrderReadyForPickupEventPublisher(
            ProductionBatchClient productionBatchClient,
            OrderClient orderClient,
            CustomerClient customerClient,
            NotificationClient notificationClient
    ) {
        this.productionBatchClient = productionBatchClient;
        this.orderClient = orderClient;
        this.customerClient = customerClient;
        this.notificationClient = notificationClient;
    }

    @Async
    public void publishIfReadyForPickup(String batchId, TankCode tankCode) {
        try {
            ProductionBatchResponse batch = productionBatchClient.getBatch(batchId);
            if (batch.getOrderId() == null) {
                log.warn("Skipping ready notification for batch {} because orderId is missing.", batchId);
                return;
            }

            OrderResponse order = orderClient.getOrderById(batch.getOrderId());
            if (!isReadyForPickup(order, batch.getOrderItemId())) {
                log.debug("Skipping ready notification for order {} because it is not READY_FOR_PICKUP yet.", batch.getOrderId());
                return;
            }

            if (order.getCustomerId() == null) {
                log.warn("Skipping ready notification for order {} because customerId is missing.", batch.getOrderId());
                return;
            }

            CustomerResponse customer = customerClient.getCustomerById(order.getCustomerId());
            OrderReadyForPickupEvent event = new OrderReadyForPickupEvent(
                    batch.getOrderId(),
                    customerName(customer),
                    customer.getPhoneNumber(),
                    tankCode.name(),
                    READY_FOR_PICKUP
            );

            notificationClient.publishOrderReadyForPickup(event);
        } catch (Exception ex) {
            log.warn("Failed to publish ready-for-pickup notification event for batch {}: {}", batchId, ex.getMessage());
        }
    }

    private boolean isReadyForPickup(OrderResponse order, Long orderItemId) {
        if (isReady(order.getStatus())) {
            return true;
        }

        if (order.getItems() == null || orderItemId == null) {
            return false;
        }

        return order.getItems().stream()
                .anyMatch(item -> orderItemId.equals(item.getId()) && isReady(item.getStatus()));
    }

    private boolean isReady(String status) {
        return status != null && READY_FOR_PICKUP.equals(status.trim().toUpperCase(Locale.ROOT));
    }

    private String customerName(CustomerResponse customer) {
        String firstName = customer.getFirstName() == null ? "" : customer.getFirstName().trim();
        String lastName = customer.getLastName() == null ? "" : customer.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }
}

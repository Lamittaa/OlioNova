package com.ops.tracking_service.service;

import com.ops.tracking_service.client.CustomerClient;
import com.ops.tracking_service.client.NotificationClient;
import com.ops.tracking_service.client.OrderClient;
import com.ops.tracking_service.client.ProductionBatchClient;
import com.ops.tracking_service.dto.CustomerResponse;
import com.ops.tracking_service.dto.OrderItemResponse;
import com.ops.tracking_service.dto.OrderReadyForPickupEvent;
import com.ops.tracking_service.dto.OrderResponse;
import com.ops.tracking_service.dto.ProductionBatchResponse;
import com.ops.tracking_service.model.TankCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReadyForPickupEventPublisherTest {

    @Mock
    private ProductionBatchClient productionBatchClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private OrderReadyForPickupEventPublisher publisher;

    @Test
    void publishesReadyEventWhenTankAssignedAndOrderItemIsReady() {
        when(productionBatchClient.getBatch("B-1")).thenReturn(batch());
        when(orderClient.getOrderById(29L)).thenReturn(order("PAID", "READY_FOR_PICKUP"));
        when(customerClient.getCustomerById(7L)).thenReturn(customer());

        publisher.publishIfReadyForPickup("B-1", TankCode.C);

        ArgumentCaptor<OrderReadyForPickupEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForPickupEvent.class);
        verify(notificationClient).publishOrderReadyForPickup(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOrderId()).isEqualTo(29L);
        assertThat(eventCaptor.getValue().getCustomerName()).isEqualTo("Ahmad Khalil");
        assertThat(eventCaptor.getValue().getCustomerPhone()).isEqualTo("+972500000000");
        assertThat(eventCaptor.getValue().getTankNumber()).isEqualTo("C");
        assertThat(eventCaptor.getValue().getOrderStatus()).isEqualTo("READY_FOR_PICKUP");
    }

    @Test
    void doesNotPublishIfOrderIsNotReady() {
        when(productionBatchClient.getBatch("B-1")).thenReturn(batch());
        when(orderClient.getOrderById(29L)).thenReturn(order("IN_PROGRESS", "IN_PROGRESS"));

        publisher.publishIfReadyForPickup("B-1", TankCode.C);

        verify(notificationClient, never()).publishOrderReadyForPickup(org.mockito.Mockito.any());
    }

    private ProductionBatchResponse batch() {
        ProductionBatchResponse batch = new ProductionBatchResponse();
        batch.setBatchId("B-1");
        batch.setOrderId(29L);
        batch.setOrderItemId(101L);
        return batch;
    }

    private OrderResponse order(String orderStatus, String itemStatus) {
        OrderItemResponse item = new OrderItemResponse();
        item.setId(101L);
        item.setStatus(itemStatus);

        OrderResponse order = new OrderResponse();
        order.setId(29L);
        order.setCustomerId(7L);
        order.setStatus(orderStatus);
        order.setItems(List.of(item));
        return order;
    }

    private CustomerResponse customer() {
        CustomerResponse customer = new CustomerResponse();
        customer.setId(7L);
        customer.setFirstName("Ahmad");
        customer.setLastName("Khalil");
        customer.setPhoneNumber("+972500000000");
        return customer;
    }
}

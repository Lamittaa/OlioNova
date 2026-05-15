package com.ops.notification_service.event;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import com.ops.notification_service.service.SmsNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderReadyForPickupEventListenerTest {

    @Test
    void listenerIsAsynchronousAndDelegatesToNotificationService() throws Exception {
        Method method = OrderReadyForPickupEventListener.class
                .getMethod("onOrderReadyForPickup", OrderReadyForPickupEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();

        SmsNotificationService smsNotificationService = mock(SmsNotificationService.class);
        OrderReadyForPickupEvent event = new OrderReadyForPickupEvent();
        OrderReadyForPickupEventListener listener = new OrderReadyForPickupEventListener(smsNotificationService);

        listener.onOrderReadyForPickup(event);

        verify(smsNotificationService).handleOrderReadyForPickup(event);
    }
}

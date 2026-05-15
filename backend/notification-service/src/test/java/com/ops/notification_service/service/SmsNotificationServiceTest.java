package com.ops.notification_service.service;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import com.ops.notification_service.model.SmsLog;
import com.ops.notification_service.model.SmsStatus;
import com.ops.notification_service.provider.SmsProvider;
import com.ops.notification_service.provider.SmsSendResult;
import com.ops.notification_service.repository.SmsLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsNotificationServiceTest {

    @Mock
    private SmsLogRepository smsLogRepository;

    @Mock
    private SmsProvider smsProvider;

    @InjectMocks
    private SmsNotificationService service;

    @Test
    void sendsSmsWhenOrderReadyEventIsValid() {
        OrderReadyForPickupEvent event = validEvent();
        when(smsProvider.send(eq(event.getCustomerPhone()), any(String.class)))
                .thenReturn(SmsSendResult.success("SM123"));

        service.handleOrderReadyForPickup(event);

        verify(smsProvider).send(eq("+972500000000"), any(String.class));
        ArgumentCaptor<SmsLog> logCaptor = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(SmsStatus.SUCCESS);
        assertThat(logCaptor.getValue().isSmsSent()).isTrue();
    }

    @Test
    void doesNotSendIfSmsWasAlreadySent() {
        OrderReadyForPickupEvent event = validEvent();
        when(smsLogRepository.existsByOrderIdAndSmsSentTrue(event.getOrderId())).thenReturn(true);

        service.handleOrderReadyForPickup(event);

        verify(smsProvider, never()).send(any(), any());
        verify(smsLogRepository, never()).save(any());
    }

    @Test
    void doesNotSendIfPhoneIsInvalid() {
        OrderReadyForPickupEvent event = validEvent();
        event.setCustomerPhone("0592998877");

        service.handleOrderReadyForPickup(event);

        verify(smsProvider, never()).send(any(), any());
        verify(smsLogRepository, never()).save(any());
    }

    @Test
    void providerFailureIsHandledSafely() {
        OrderReadyForPickupEvent event = validEvent();
        when(smsProvider.send(eq(event.getCustomerPhone()), any(String.class)))
                .thenReturn(SmsSendResult.failure("provider down"));

        service.handleOrderReadyForPickup(event);

        ArgumentCaptor<SmsLog> logCaptor = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(SmsStatus.FAILED);
        assertThat(logCaptor.getValue().isSmsSent()).isFalse();
    }

    @Test
    void retriesOnceAfterProviderException() {
        OrderReadyForPickupEvent event = validEvent();
        doThrow(new RuntimeException("temporary error"))
                .doReturn(SmsSendResult.success("SM456"))
                .when(smsProvider).send(eq(event.getCustomerPhone()), any(String.class));

        service.handleOrderReadyForPickup(event);

        verify(smsProvider, org.mockito.Mockito.times(2)).send(eq(event.getCustomerPhone()), any(String.class));
        ArgumentCaptor<SmsLog> logCaptor = ArgumentCaptor.forClass(SmsLog.class);
        verify(smsLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(SmsStatus.SUCCESS);
    }

    private OrderReadyForPickupEvent validEvent() {
        OrderReadyForPickupEvent event = new OrderReadyForPickupEvent();
        event.setOrderId(29L);
        event.setCustomerName("Ahmad Khalil");
        event.setCustomerPhone("+972500000000");
        event.setTankNumber("C");
        event.setOrderStatus("READY_FOR_PICKUP");
        return event;
    }
}

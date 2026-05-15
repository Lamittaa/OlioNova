package com.ops.notification_service.controller;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import com.ops.notification_service.provider.SmsProvider;
import com.ops.notification_service.provider.SmsSendResult;
import com.ops.notification_service.repository.SmsLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.api-key.internal=test-internal-key",
        "twilio.account-sid=test",
        "twilio.auth-token=test",
        "twilio.from-number=+10000000000"
})
class NotificationEventControllerIntegrationTest {

    @Autowired
    private NotificationEventController controller;

    @Autowired
    private SmsLogRepository smsLogRepository;

    @MockitoBean
    private SmsProvider smsProvider;

    @Test
    void receivesReadyEventAndProcessesItAsynchronously() throws Exception {
        when(smsProvider.send(eq("+972500000000"), any(String.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(300);
                    return SmsSendResult.success("SM789");
                });

        OrderReadyForPickupEvent event = readyEvent();
        long startedAt = System.currentTimeMillis();

        ResponseEntity<Void> response = controller.publishOrderReadyForPickup("test-internal-key", event);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(System.currentTimeMillis() - startedAt).isLessThan(300);
        waitForSmsLog();
        assertThat(smsLogRepository.existsByOrderIdAndSmsSentTrue(29L)).isTrue();
    }

    @Test
    void rejectsEventWhenInternalApiKeyIsMissing() {
        ResponseEntity<Void> response = controller.publishOrderReadyForPickup(null, readyEvent());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void waitForSmsLog() throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (smsLogRepository.existsByOrderIdAndSmsSentTrue(29L)) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private OrderReadyForPickupEvent readyEvent() {
        OrderReadyForPickupEvent event = new OrderReadyForPickupEvent();
        event.setOrderId(29L);
        event.setCustomerName("Ahmad Khalil");
        event.setCustomerPhone("+972500000000");
        event.setTankNumber("C");
        event.setOrderStatus("READY_FOR_PICKUP");
        return event;
    }
}

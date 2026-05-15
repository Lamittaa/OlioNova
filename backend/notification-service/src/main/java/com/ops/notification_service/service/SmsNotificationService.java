package com.ops.notification_service.service;

import com.ops.notification_service.dto.OrderReadyForPickupEvent;
import com.ops.notification_service.model.SmsLog;
import com.ops.notification_service.model.SmsStatus;
import com.ops.notification_service.provider.SmsProvider;
import com.ops.notification_service.provider.SmsSendResult;
import com.ops.notification_service.repository.SmsLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);
    private static final Pattern E164_PHONE = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final String READY_FOR_PICKUP = "READY_FOR_PICKUP";

    private final SmsLogRepository smsLogRepository;
    private final SmsProvider smsProvider;

    public SmsNotificationService(SmsLogRepository smsLogRepository, SmsProvider smsProvider) {
        this.smsLogRepository = smsLogRepository;
        this.smsProvider = smsProvider;
    }

    @Transactional
    public void handleOrderReadyForPickup(OrderReadyForPickupEvent event) {
        ValidationResult validation = validate(event);
        if (!validation.valid()) {
            log.warn("Skipping ready-for-pickup SMS: {}", validation.reason());
            return;
        }

        if (smsLogRepository.existsByOrderIdAndSmsSentTrue(event.getOrderId())) {
            log.info("Skipping ready-for-pickup SMS for order {} because it was already sent.", event.getOrderId());
            return;
        }

        String message = buildArabicReadyMessage(event);
        SmsSendResult result = sendWithSingleRetry(event.getCustomerPhone(), message);
        SmsStatus status = result.success() ? SmsStatus.SUCCESS : SmsStatus.FAILED;
        smsLogRepository.save(new SmsLog(
                event.getOrderId(),
                event.getCustomerPhone(),
                status,
                truncate(result.providerResponse()),
                result.success()
        ));
    }

    private SmsSendResult sendWithSingleRetry(String phone, String message) {
        SmsSendResult firstAttempt = sendSafely(phone, message);
        if (firstAttempt.success()) {
            return firstAttempt;
        }

        log.warn("SMS provider failed on first attempt. Retrying once.");
        return sendSafely(phone, message);
    }

    private SmsSendResult sendSafely(String phone, String message) {
        try {
            return smsProvider.send(phone, message);
        } catch (Exception ex) {
            log.warn("SMS provider threw an exception: {}", ex.getMessage());
            return SmsSendResult.failure(ex.getMessage());
        }
    }

    private ValidationResult validate(OrderReadyForPickupEvent event) {
        if (event == null) {
            return ValidationResult.invalid("event is missing");
        }
        if (event.getOrderId() == null) {
            return ValidationResult.invalid("orderId is missing");
        }
        if (isBlank(event.getCustomerName())) {
            return ValidationResult.invalid("customerName is missing for order " + event.getOrderId());
        }
        if (isBlank(event.getCustomerPhone())) {
            return ValidationResult.invalid("customerPhone is missing for order " + event.getOrderId());
        }
        if (!E164_PHONE.matcher(event.getCustomerPhone().trim()).matches()) {
            return ValidationResult.invalid("customerPhone is not valid E.164 for order " + event.getOrderId());
        }
        if (isBlank(event.getTankNumber())) {
            return ValidationResult.invalid("tankNumber is missing for order " + event.getOrderId());
        }
        if (!READY_FOR_PICKUP.equals(normalize(event.getOrderStatus()))) {
            return ValidationResult.invalid("order is not READY_FOR_PICKUP for order " + event.getOrderId());
        }

        return ValidationResult.ok();
    }

    private String buildArabicReadyMessage(OrderReadyForPickupEvent event) {
        return "مرحباً " + event.getCustomerName().trim()
                + "، زيت الزيتون الخاص بك أصبح جاهزاً. يرجى التوجه إلى الخزان رقم "
                + event.getTankNumber().trim()
                + " لاستلامه. رقم الطلب: "
                + event.getOrderId();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }

    private record ValidationResult(boolean valid, String reason) {
        static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}

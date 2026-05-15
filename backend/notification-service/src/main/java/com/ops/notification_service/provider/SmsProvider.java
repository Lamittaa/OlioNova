package com.ops.notification_service.provider;

public interface SmsProvider {

    SmsSendResult send(String phone, String message);
}

package com.ops.notification_service.service;

import com.ops.notification_service.provider.SmsProvider;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsProvider smsProvider;

    public SmsService(SmsProvider smsProvider) {
        this.smsProvider = smsProvider;
    }

    public void sendSms(String phone, String message) {
        smsProvider.send(phone, message);
    }
}

package com.ops.notification_service.service;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.messages.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final VonageClient client;
    private final String from;

    public SmsService(
            @Value("${vonage.api-key}") String apiKey,
            @Value("${vonage.api-secret}") String apiSecret,
            @Value("${vonage.from}") String from
    ) {

        this.client = VonageClient.builder()
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .build();

        this.from = from;
    }

    public void sendSms(String phone, String message) {

        TextMessage sms = new TextMessage(
                from,
                phone,
                message
        );

        client.getSmsClient().submitMessage(sms);

        System.out.println("SMS SENT SUCCESS");
    }
}
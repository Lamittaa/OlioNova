package com.ops.notification_service.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TwilioSmsProvider implements SmsProvider {

    private final RestTemplate restTemplate;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsProvider(
            RestTemplate restTemplate,
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber
    ) {
        this.restTemplate = restTemplate;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public SmsSendResult send(String phone, String message) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", fromNumber);
        body.add("To", phone);
        body.add("Body", message);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        return response.getStatusCode().is2xxSuccessful()
                ? SmsSendResult.success(response.getBody())
                : SmsSendResult.failure(response.getBody());
    }

    private String basicAuthToken() {
        String value = accountSid + ":" + authToken;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

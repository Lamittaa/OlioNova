package com.ops.notification_service.controller;

import com.ops.notification_service.service.SmsService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class NotificationController {

    private final SmsService smsService;

    @PostMapping("/send")
    public String sendSms(
            @RequestParam String phone,
            @RequestParam String message
    ) {

        smsService.sendSms(phone, message);

        return "SMS sent successfully";
    }
}
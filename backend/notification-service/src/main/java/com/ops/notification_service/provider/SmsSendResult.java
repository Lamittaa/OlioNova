package com.ops.notification_service.provider;

public record SmsSendResult(boolean success, String providerResponse) {

    public static SmsSendResult success(String providerResponse) {
        return new SmsSendResult(true, providerResponse);
    }

    public static SmsSendResult failure(String providerResponse) {
        return new SmsSendResult(false, providerResponse);
    }
}

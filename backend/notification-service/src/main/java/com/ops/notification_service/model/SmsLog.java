package com.ops.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_log")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SmsStatus status;

    @Column(name = "provider_response", length = 2000)
    private String providerResponse;

    @Column(name = "sms_sent", nullable = false)
    private boolean smsSent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public SmsLog() {
    }

    public SmsLog(Long orderId, String phone, SmsStatus status, String providerResponse, boolean smsSent) {
        this.orderId = orderId;
        this.phone = phone;
        this.status = status;
        this.providerResponse = providerResponse;
        this.smsSent = smsSent;
    }

    @PrePersist
    void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPhone() {
        return phone;
    }

    public SmsStatus getStatus() {
        return status;
    }

    public String getProviderResponse() {
        return providerResponse;
    }

    public boolean isSmsSent() {
        return smsSent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

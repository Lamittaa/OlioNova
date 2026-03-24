package com.project.payment.dto;

import com.project.payment.model.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PaymentResponse {

    private Long              id;
    private Long              orderId;
    private PaymentType       paymentType;
    private LocalDateTime     paymentDate;
    private Long              userId;

    // تفاصيل كل item
    private List<PaymentItemDetail> items;

    // السعر الإجمالي
    private BigDecimal        totalPrice;
}
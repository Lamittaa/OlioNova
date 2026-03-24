package com.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
// ✅ يخفي أي حقل قيمته null تلقائياً
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentItemDetail {

    private String     productName;
    private String     productType;

    // SERVICE فقط — لن يظهر للجالون والجيفت
    private String     oliveType;
    private Integer    bagsCount;

    // الكمية — كغ للعصر والجيفت، قطعة للجالون
    private Integer    quantity;

    // سعر الـ item
    private BigDecimal Total;
}
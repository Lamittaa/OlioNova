package com.project.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class EmployeePaymentSummary {

    private Long userId;
    private long count;
    private BigDecimal total;
}

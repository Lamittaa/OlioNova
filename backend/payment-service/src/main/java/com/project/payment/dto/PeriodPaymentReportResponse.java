package com.project.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PeriodPaymentReportResponse {

    private LocalDate from;
    private LocalDate to;
    private int totalPayments;
    private BigDecimal totalAmount;
}

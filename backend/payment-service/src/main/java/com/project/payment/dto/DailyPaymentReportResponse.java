package com.project.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyPaymentReportResponse {

    private LocalDate date;
    private int totalPayments;
    private BigDecimal totalAmount;
    private List<EmployeePaymentSummary> byEmployee;
}

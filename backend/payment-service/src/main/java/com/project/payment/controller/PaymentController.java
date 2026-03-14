package com.project.payment.controller;

import com.project.payment.dto.CreatePaymentRequest;
import com.project.payment.dto.DailyPaymentReportResponse;
import com.project.payment.dto.PaymentResponse;
import com.project.payment.dto.PeriodPaymentReportResponse;
import com.project.payment.service.PaymentExcelExportService;
import com.project.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentExcelExportService excelService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("USER = {}", auth.getName());
        log.info("AUTHORITIES = {}", auth.getAuthorities());

        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createCashPayment(request));
    }

  
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable @Min(1) Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentById(paymentId)
        );
    }


    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable @Min(1) Long orderId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentByOrderId(orderId)
        );
    }


    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<DailyPaymentReportResponse> dailyReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                paymentService.getDailyReport(date)
        );
    }

    @GetMapping("/period")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PeriodPaymentReportResponse> periodReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ResponseEntity.ok(
                paymentService.getPeriodReport(from, to)
        );
    }

    @GetMapping("/reports/daily/excel")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportDailyExcel(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        byte[] file = excelService.exportDaily(date);

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=daily-payments-" + date + ".xlsx"
                )
                .body(file);
    }

   
    @GetMapping("/reports/period/excel")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportPeriodExcel(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        byte[] file = excelService.exportPeriod(from, to);

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=payments-" + from + "-to-" + to + ".xlsx"
                )
                .body(file);
    }
}

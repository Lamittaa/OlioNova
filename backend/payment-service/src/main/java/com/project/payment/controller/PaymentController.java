package com.project.payment.controller;

import com.project.payment.dto.CreatePaymentRequest;
import com.project.payment.dto.PaymentResponse;
import com.project.payment.service.PaymentExcelExportService;
import com.project.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // =====================================================
    // GET ALL PAYMENTS
    // =====================================================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        log.info("USER = {}", auth.getName());
        log.info("AUTHORITIES = {}", auth.getAuthorities());

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }

    // =====================================================
    // CREATE PAYMENT
    // =====================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paymentService.createCashPayment(request));
    }

    // =====================================================
    // GET PAYMENT BY ID
    // =====================================================
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable @Min(1) Long paymentId
    ) {

        return ResponseEntity.ok(
                paymentService.getPaymentById(paymentId)
        );
    }

    // =====================================================
    // GET PAYMENT BY ORDER ID
    // =====================================================
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable @Min(1) Long orderId
    ) {

        return ResponseEntity.ok(
                paymentService.getPaymentByOrderId(orderId)
        );
    }

    // =====================================================
    // DAILY EXCEL REPORT
    // =====================================================
   @GetMapping("/reports/daily/excel")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
public ResponseEntity<byte[]> exportDailyExcel() {

    // ✅ تاريخ اليوم تلقائياً — لا يحتاج المستخدم يكتب شيئاً
    LocalDate today = LocalDate.now();

    byte[] file = excelService.exportDaily(today);

    return ResponseEntity.ok()
            .header(
                    "Content-Disposition",
                    "attachment; filename=daily-payments-"
                    + today + ".xlsx")
            .body(file);
}

    // =====================================================
    // PERIOD EXCEL REPORT
    // =====================================================
   @GetMapping("/reports/period/excel")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
public ResponseEntity<byte[]> exportPeriodExcel(
        @RequestParam String from,
        @RequestParam String to
) {
    LocalDate fromDate = LocalDate.parse(from.trim());
    LocalDate toDate   = LocalDate.parse(to.trim());

    byte[] file = excelService.exportPeriod(fromDate, toDate);

    return ResponseEntity.ok()
            .header(
                    "Content-Disposition",
                    "attachment; filename=payments-"
                    + fromDate + "-to-" + toDate + ".xlsx")
            .body(file);
}

    
}
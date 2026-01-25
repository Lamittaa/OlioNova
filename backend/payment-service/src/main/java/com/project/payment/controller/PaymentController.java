package com.project.payment.controller;

import com.project.payment.dto.CreatePaymentRequest;
import com.project.payment.dto.PaymentResponse;
import com.project.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ✅ Create Cash Payment
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createCashPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

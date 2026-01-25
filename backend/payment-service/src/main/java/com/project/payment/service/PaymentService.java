package com.project.payment.service;

import com.project.payment.client.OrderClient;
import com.project.payment.dto.CreatePaymentRequest;
import com.project.payment.dto.PaymentResponse;
import com.project.payment.model.Payment;
import com.project.payment.model.PaymentType;
import com.project.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;

    public PaymentResponse createCashPayment(CreatePaymentRequest request) {

        // 1️⃣ نجيب employeeId من JWT
        JwtAuthenticationToken auth =
                (JwtAuthenticationToken) SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Long employeeId = auth.getToken().getClaim("user_id");

        // 2️⃣ نتأكد إن الطلب موجود
        orderClient.getOrderById(request.getOrderId());

        // 3️⃣ ننشئ Payment
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setTotalPrice(request.getTotalPrice());
        payment.setPaymentType(PaymentType.CASH);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setEmployeeId(employeeId);

        Payment saved = paymentRepository.save(payment);

        // 4️⃣ نحدّث حالة الطلب إلى PAID
        orderClient.markOrderAsPaid(request.getOrderId());

        // 5️⃣ نرجّع Response
        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse r = new PaymentResponse();
        r.setId(payment.getId());
        r.setOrderId(payment.getOrderId());
        r.setTotalPrice(payment.getTotalPrice());
        r.setPaymentType(payment.getPaymentType());
        r.setPaymentDate(payment.getPaymentDate());
        r.setEmployeeId(payment.getEmployeeId());
        return r;
    }
}

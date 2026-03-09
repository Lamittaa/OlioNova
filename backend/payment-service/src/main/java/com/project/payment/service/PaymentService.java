package com.project.payment.service;

import lombok.extern.slf4j.Slf4j;

import com.project.payment.client.OrderClient;
import com.project.payment.client.QueueClient;
import com.project.payment.dto.CreatePaymentRequest;
import com.project.payment.dto.DailyPaymentReportResponse;
import com.project.payment.dto.EmployeePaymentSummary;
import com.project.payment.dto.OrderSummaryResponse;
import com.project.payment.dto.PaymentResponse;
import com.project.payment.dto.PeriodPaymentReportResponse;
import com.project.payment.exception.BusinessException;
import com.project.payment.exception.OrderNotEditableException;
import com.project.payment.exception.ResourceNotFoundException;
import com.project.payment.exception.PaymentAlreadyExistsException;
import com.project.payment.model.Payment;
import com.project.payment.model.PaymentType;
import com.project.payment.repository.PaymentRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
private final QueueClient queueClient;

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;

    // =========================================================
    // 1️⃣ CREATE PAYMENT
    // =========================================================
public PaymentResponse createCashPayment(CreatePaymentRequest request) {

    // ---------- AUTH ----------
    JwtAuthenticationToken auth =
            (JwtAuthenticationToken) SecurityContextHolder
                    .getContext()
                    .getAuthentication();

    Long employeeId = auth.getToken().getClaim("employeeId");

    if (employeeId == null) {
        throw new BusinessException("employeeId missing in authentication token");
    }

    // ---------- FETCH ORDER ----------
    OrderSummaryResponse order;
    try {
        order = orderClient.getOrderById(request.getOrderId());
    } catch (FeignException.NotFound e) {
        throw new ResourceNotFoundException(
                "Order not found with id: " + request.getOrderId()
        );
    }

    Long orderId = order.id();

    // ---------- PREVENT DOUBLE PAYMENT ----------
    if (paymentRepository.findByOrderId(orderId).isPresent()) {
        throw new PaymentAlreadyExistsException(
                "Payment already exists for orderId: " + orderId
        );
    }

    // ---------- VALIDATE ORDER STATUS ----------
    if (!"SUBMITTED".equals(order.status())) {
        throw new OrderNotEditableException(
                "Order cannot be paid in status: " + order.status()
        );
    }

    // ---------- CREATE PAYMENT ----------
    Payment payment = new Payment();
    payment.setOrderId(orderId);
    payment.setTotalPrice(order.totalPrice());
    payment.setPaymentType(PaymentType.CASH);
    payment.setPaymentDate(LocalDateTime.now());
    payment.setEmployeeId(employeeId);

    Payment saved = paymentRepository.save(payment);

    // ---------- UPDATE ORDER STATUS ----------
    try {
        orderClient.updateOrderStatus(
                orderId,
                Map.of("status", "PAID")
        );
    } catch (FeignException e) {

        if (e.status() == 400 || e.status() == 409) {
            throw new BusinessException(
                    "Order cannot be paid in its current status"
            );
        }

        throw e;
    }

    // ---------- ADD TO PRODUCTION QUEUE ----------
    try {
        
    } catch (Exception e) {
        log.error("Failed to add order to production queue", e);
        throw new BusinessException(
                "Payment succeeded but failed to add to production queue"
        );
    }

    return map(saved);
}


    // =========================================================
    // 2️⃣ GET PAYMENT BY ID
    // =========================================================
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with id: " + paymentId
                        )
                );

        return map(payment);
    }

    // =========================================================
    // 3️⃣ GET ALL PAYMENTS
    // =========================================================
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    // =========================================================
    // 4️⃣ GET PAYMENT BY ORDER ID
    // =========================================================
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for orderId: " + orderId
                        )
                );

        return map(payment);
    }

    // =========================================================
    // 🔁 MAPPER
    // =========================================================
    private PaymentResponse map(Payment p) {

        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setTotalPrice(p.getTotalPrice());
        r.setPaymentType(p.getPaymentType());
        r.setPaymentDate(p.getPaymentDate());
        r.setEmployeeId(p.getEmployeeId());

        return r;
    }

    // =========================================================
    // 📊 DAILY REPORT
    // =========================================================
    public DailyPaymentReportResponse getDailyReport(LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        BigDecimal totalAmount =
                paymentRepository.sumTotalBetween(start, end);

        Map<Long, List<Payment>> byEmployee =
                payments.stream().collect(
                        java.util.stream.Collectors.groupingBy(
                                Payment::getEmployeeId
                        )
                );

        List<EmployeePaymentSummary> employeeSummaries =
                byEmployee.entrySet().stream()
                        .map(e -> new EmployeePaymentSummary(
                                e.getKey(),
                                e.getValue().size(),
                                e.getValue().stream()
                                        .map(Payment::getTotalPrice)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        ))
                        .toList();

        DailyPaymentReportResponse response =
                new DailyPaymentReportResponse();

        response.setDate(date);
        response.setTotalPayments(payments.size());
        response.setTotalAmount(totalAmount);
        response.setByEmployee(employeeSummaries);

        return response;
    }

    // =========================================================
    // 📅 PERIOD REPORT
    // =========================================================
    public PeriodPaymentReportResponse getPeriodReport(
            LocalDate from,
            LocalDate to
    ) {

        if (from.isAfter(to)) {
            throw new BusinessException(
                    "From date must be before To date"
            );
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        List<Payment> payments =
                paymentRepository.findByPaymentDateBetween(start, end);

        BigDecimal totalAmount =
                paymentRepository.sumTotalBetween(start, end);

        PeriodPaymentReportResponse response =
                new PeriodPaymentReportResponse();

        response.setFrom(from);
        response.setTo(to);
        response.setTotalPayments(payments.size());
        response.setTotalAmount(totalAmount);

        return response;
    }
}

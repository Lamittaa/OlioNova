package com.project.payment.service;

import lombok.extern.slf4j.Slf4j;

import com.project.payment.client.OrderClient;
import com.project.payment.client.ProductClient;
import com.project.payment.client.QueueClient;
import com.project.payment.dto.*;
import com.project.payment.exception.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final QueueClient       queueClient;
    private final PaymentRepository paymentRepository;
    private final OrderClient       orderClient;
    private final ProductClient     productClient;

    // =====================================================
    // CREATE CASH PAYMENT
    // =====================================================
    public PaymentResponse createCashPayment(CreatePaymentRequest request) {

        // 1. استخرج userId من الـ JWT
        JwtAuthenticationToken auth =
                (JwtAuthenticationToken) SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Long userId = auth.getToken().getClaim("userId");

        if (userId == null) {
            throw new BusinessException(
                    "userId missing in authentication token");
        }

        // 2. جلب الطلب من order-service
        OrderSummaryResponse order;

        try {
            order = orderClient.getOrderById(request.getOrderId());
        }
        catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "Order not found with id: " + request.getOrderId());
        }
        catch (FeignException e) {
            log.error("Order service unavailable", e);
            throw new ServiceUnavailableException(
                    "Order service unavailable");
        }

        Long orderId = order.getId();

        // 3. منع الدفع مرتين
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new PaymentAlreadyExistsException(
                    "Payment already exists for orderId: " + orderId);
        }

        // 4. يجب أن يكون الطلب SUBMITTED
        if (!"SUBMITTED".equals(order.getStatus())) {
            throw new OrderNotEditableException(
                    "Order cannot be paid in status: " + order.getStatus());
        }

        // 5. احسب السعر الكلي
        BigDecimal totalPrice = BigDecimal.ZERO;
        boolean    hasService = false;

        for (OrderItemSummary item : order.getItems()) {

            ProductResponse product;

            try {
                product = productClient.getProduct(item.getProductId());
            }
            catch (FeignException.NotFound e) {
                throw new ResourceNotFoundException(
                        "Product not found with id: " + item.getProductId());
            }
            catch (FeignException e) {
                log.error("Product service unavailable", e);
                throw new ServiceUnavailableException(
                        "Product service unavailable");
            }

            // -------------------------------------------
            // SERVICE — عصر الزيتون
            // weight × rate (0.4 / 0.6 / 0.8)
            // -------------------------------------------
            if ("SERVICE".equalsIgnoreCase(product.getProductType())) {

                hasService = true;

                double  weight   = item.getQuantity();
                boolean isMember = order.isMember();

                totalPrice = totalPrice.add(
                        calculateKgPrice(weight, isMember));
            }

            // -------------------------------------------
            // PURCHASE KG — Olive Gift
            // نفس منطق العصر تماماً
            // -------------------------------------------
            else if ("PURCHASE".equalsIgnoreCase(product.getProductType())
                    && "KG".equalsIgnoreCase(product.getUnit())) {

                double  weight   = item.getQuantity();
                boolean isMember = order.isMember();

                totalPrice = totalPrice.add(
                        calculateKgPrice(weight, isMember));
            }

            // -------------------------------------------
            // PURCHASE PIECE — Olive Oil Gallon
            // سعر ثابت من DB × الكمية
            // -------------------------------------------
            else {

                // تحقق من المخزون
                if (product.getInventory() == null
                        || product.getInventory() < item.getQuantity()) {
                    throw new BusinessException(
                            "Not enough stock for: "
                            + product.getProductName()
                            + ". Available: " + product.getInventory()
                            + ", Requested: " + item.getQuantity());
                }

                BigDecimal lineTotal =
                        product.getPrice()
                               .multiply(BigDecimal.valueOf(
                                       item.getQuantity()));

                totalPrice = totalPrice.add(lineTotal);
            }
        }

        // 6. احفظ الدفعة
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setTotalPrice(totalPrice);
        payment.setPaymentType(PaymentType.CASH);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setUserId(userId);

        Payment saved = paymentRepository.save(payment);

        // 7. حدّث حالة الطلب إلى PAID
        try {
            orderClient.updateOrderStatus(
                    orderId,
                    Map.of("status", "PAID"));
        }
        catch (FeignException e) {
            log.error("Order service unavailable after saving payment id={}",
                    saved.getId(), e);
            throw new ServiceUnavailableException(
                    "Order service unavailable");
        }

        // 8. إصدار تذكرة طابور إذا كان هناك خدمة عصر
        if (hasService) {
            try {
                queueClient.issueProductionTicket(orderId);
            }
            catch (FeignException e) {
                log.error("Queue service unavailable for orderId={}",
                        orderId, e);
                throw new ServiceUnavailableException(
                        "Queue service must be running for pressing orders");
            }
        }

        return map(saved);
    }

    // =====================================================
    // GET PAYMENT BY ID
    // =====================================================
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));

        return map(payment);
    }

    // =====================================================
    // GET PAYMENT BY ORDER ID
    // =====================================================
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for orderId: " + orderId));

        return map(payment);
    }

    // =====================================================
    // GET ALL PAYMENTS
    // =====================================================
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    // =====================================================
    // CALCULATE KG PRICE
    // مساهم        → weight × 0.4  دائماً
    // غير مساهم   → weight × 0.8  إذا < 100 كغ
    //             → weight × 0.6  إذا ≥ 100 كغ
    // =====================================================
    private BigDecimal calculateKgPrice(double weight, boolean isMember) {

        double rate;

        if (isMember) {
            rate = 0.4;
        }
        else {
            rate = (weight >= 100) ? 0.6 : 0.8;
        }

        return BigDecimal.valueOf(weight * rate);
    }

    // =====================================================
    // MAP ENTITY → RESPONSE
    // =====================================================
    private PaymentResponse map(Payment p) {

        PaymentResponse r = new PaymentResponse();

        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setTotalPrice(p.getTotalPrice());
        r.setPaymentType(p.getPaymentType());
        r.setPaymentDate(p.getPaymentDate());
        r.setUserId(p.getUserId());

        return r;
    }
}
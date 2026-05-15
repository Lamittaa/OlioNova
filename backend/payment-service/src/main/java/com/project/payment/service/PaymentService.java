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
import java.util.ArrayList;
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
    BigDecimal              totalPrice  = BigDecimal.ZERO;
    boolean                 hasService  = false;
    List<PaymentItemDetail> itemDetails = new ArrayList<>();

    for (OrderItemSummary item : order.getItems()) {

        ProductResponse product;

        try {
            product = productClient.getProduct(item.getProductId());
        }
        catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "Product not found with id: "
                    + item.getProductId());
        }
        catch (FeignException e) {
            log.error("Product service unavailable", e);
            throw new ServiceUnavailableException(
                    "Product service unavailable");
        }

        // -------------------------------------------
        // SERVICE — عصر الزيتون
        // -------------------------------------------
        String productType = product.getProductType() == null
                ? ""
                : product.getProductType();

        if (isOlivePressingProduct(productType)) {

            hasService = true;

            double     weight    = item.getQuantity();
            boolean    isMember  = order.isMember();
            BigDecimal lineTotal = calculateKgPrice(weight, isMember);

            totalPrice = totalPrice.add(lineTotal);

            PaymentItemDetail detail = new PaymentItemDetail();
            detail.setProductName(product.getProductName());
            detail.setProductType(productType);
            detail.setOliveType(item.getOliveType());
            detail.setBagsCount(item.getBagsCount());
            detail.setQuantity((int) weight);
            detail.setTotal(lineTotal);
            itemDetails.add(detail);
        }

        // -------------------------------------------
        // PURCHASE — KG + PIECE
        // -------------------------------------------
        else if (isPurchaseProduct(productType)) {

            BigDecimal lineTotal;

            if ("KG".equalsIgnoreCase(product.getUnit())) {
                double  weight   = item.getQuantity();
                boolean isMember = order.isMember();
                lineTotal = calculateKgPrice(weight, isMember);
            }
            else {
                lineTotal = product.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
            }

            totalPrice = totalPrice.add(lineTotal);

            // ينقص المخزون المتاح
            try {
                DecreaseAvailabilityRequest decreaseRequest =
                        new DecreaseAvailabilityRequest();
                decreaseRequest.setQuantity(item.getQuantity());

                productClient.decreaseAvailability(
                        product.getId(),
                        decreaseRequest);
            }
            catch (FeignException e) {
                log.error("Failed to decrease availability for product id={}",
                        product.getId(), e);
                throw new ServiceUnavailableException(
                        "Product service unavailable");
            }

            PaymentItemDetail detail = new PaymentItemDetail();
            detail.setProductName(product.getProductName());
            detail.setProductType(productType);
            detail.setQuantity((int) item.getQuantity());
            detail.setTotal(lineTotal);
            itemDetails.add(detail);
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

    // 7. غير status الـ items والطلب → PAID
    try {
        orderClient.payOrder(orderId);
    }
    catch (FeignException e) {
        log.error(
                "Order service unavailable after saving payment id={}",
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
            log.warn(
                    "Queue service unavailable for orderId={}, payment will remain recorded",
                    orderId, e);
        }
    }

    return map(saved, itemDetails);
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
    private BigDecimal calculateKgPrice(double weight,
                                         boolean isMember) {
        double rate;

        if (isMember) {
            rate = 0.4;
        }
        else {
            rate = (weight >= 100) ? 0.6 : 0.8;
        }

        return BigDecimal.valueOf(weight * rate);
    }

    private boolean isOlivePressingProduct(String productType) {
        return "SERVICE".equalsIgnoreCase(productType)
                || "OLIVE".equalsIgnoreCase(productType);
    }

    private boolean isPurchaseProduct(String productType) {
        return "PURCHASE".equalsIgnoreCase(productType)
                || "JIFT".equalsIgnoreCase(productType)
                || "GALLON".equalsIgnoreCase(productType);
    }

    // =====================================================
    // MAP — مع items (عند الإنشاء)
    // =====================================================
    private PaymentResponse map(Payment p,
                                 List<PaymentItemDetail> items) {

        PaymentResponse r = new PaymentResponse();

        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setTotalPrice(p.getTotalPrice());
        r.setPaymentType(p.getPaymentType());
        r.setPaymentDate(p.getPaymentDate());
        r.setUserId(p.getUserId());
        r.setItems(items);

        return r;
    }

    // =====================================================
    // MAP — بدون items (للـ get methods)
    // =====================================================
    private PaymentResponse map(Payment p) {

        PaymentResponse r = new PaymentResponse();

        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setTotalPrice(p.getTotalPrice());
        r.setPaymentType(p.getPaymentType());
        r.setPaymentDate(p.getPaymentDate());
        r.setUserId(p.getUserId());
        r.setItems(List.of());

        return r;
    }
}

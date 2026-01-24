package com.project.order.service;

import com.project.order.client.CustomerClient;
import com.project.order.dto.AddOrderItemRequest;
import com.project.order.dto.OrderItemResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.UpdateOrderItemRequest;
import com.project.order.exception.BusinessRuleViolationException;
import com.project.order.exception.OrderNotEditableException;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.OrderItemMapper;
import com.project.order.mapper.OrderMapper;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.ProductLookup;
import com.project.order.repository.OrderItemRepo;
import com.project.order.repository.OrderRepo;
import com.project.order.repository.ProductLookupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderItemService {

    private final OrderRepo orderRepo;
    private final OrderItemRepo itemRepo;
    private final ProductLookupRepo productRepo;
    private final CustomerClient customerClient;

    private final OrderItemMapper orderItemMapper; // 🔥 مهم
    private final OrderMapper orderMapper;         // فقط لـ OrderResponse

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getItems(Long orderId) {

        ensureOrderExists(orderId);

        return itemRepo.findByOrderId(orderId)
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getItem(Long orderId, Long itemId) {

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                        ));

        return orderItemMapper.toResponse(item);
    }

    // =========================================================
    // ADD
    // =========================================================

    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId)
                );

        ensureOrderEditable(order);

        ProductLookup product = productRepo.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + request.getProductId()
                        )
                );

        validateAddItemRules(order, product, request);

        boolean isMember = customerClient
                .getMembership(order.getCustomerId())
                .isMembership();

        OrderItem item = new OrderItem();
        item.setOrder(order);

        // ===== Snapshot =====
        item.setProductId(product.getId());
        item.setProductName(product.getProductName());
        item.setProductType(product.getProductType());

        validateQuantity(request.getQuantity());
        item.setQuantity(request.getQuantity());

        item.setOliveType(request.getOliveType());
        item.setBagsCount(request.getBagsCount());
        item.setNote(request.getNote());

        BigDecimal unitPrice = computeFinalPrice(product, isMember);
        item.setPrice(unitPrice);
        item.setTotalPrice(
                unitPrice.multiply(item.getQuantity())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        item.setCreatedAt(LocalDateTime.now());

        itemRepo.save(item);

        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public OrderResponse updateItem(Long orderId, Long itemId, UpdateOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId)
                );

        ensureOrderEditable(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                        ));

        if (request.getQuantity() != null) {
            validateQuantity(request.getQuantity());
            item.setQuantity(request.getQuantity());
        }

        if (request.getOliveType() != null) {
            item.setOliveType(request.getOliveType());
        }

        if (request.getBagsCount() != null) {
            item.setBagsCount(request.getBagsCount());
        }

        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        item.setTotalPrice(
                item.getPrice()
                        .multiply(item.getQuantity())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        item.setUpdatedAt(LocalDateTime.now());

        itemRepo.save(item);

        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteItem(Long orderId, Long itemId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId)
                );

        ensureOrderInSubmittedOnly(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                        ));

        itemRepo.delete(item);

        recomputeOrderTotal(order);
    }

    // =========================================================
    // BUSINESS RULES
    // =========================================================

    

    // =========================================================
    // HELPERS
    // =========================================================

    private void ensureOrderExists(Long orderId) {
        if (!orderRepo.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }
    }

    private void ensureOrderEditable(Order order) {
        String status = order.getStatus().getStatusName().toUpperCase();

        if (!status.equals("SUBMITTED") && !status.equals("READY_FOR_PAYMENT")) {
            throw new OrderNotEditableException(
                    "Order items cannot be modified in status: " + status
            );
        }
    }

    private void ensureOrderInSubmittedOnly(Order order) {
        String status = order.getStatus().getStatusName().toUpperCase();

        if (!status.equals("SUBMITTED")) {
            throw new OrderNotEditableException(
                    "Order items can be deleted only in SUBMITTED status. Current: " + status
            );
        }
    }

    private void validateQuantity(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (qty.scale() > 2) {
            throw new IllegalArgumentException("Quantity can have at most 2 decimal places");
        }
    }

    private BigDecimal computeFinalPrice(ProductLookup product, boolean isMember) {

        BigDecimal base = product.getPrice();

        if (isMember && product.getMemberDiscount() != null) {
            BigDecimal factor = BigDecimal.ONE.subtract(product.getMemberDiscount());
            return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        return base.setScale(2, RoundingMode.HALF_UP);
    }

    private void recomputeOrderTotal(Order order) {

        BigDecimal total = itemRepo.findByOrderId(order.getId())
                .stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(total.setScale(2, RoundingMode.HALF_UP));
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);
    }
    private void validateAddItemRules(
        Order order,
        ProductLookup product,
        AddOrderItemRequest request
) {

    // ===== PURCHASE =====
    if ("PURCHASE".equalsIgnoreCase(product.getProductType())) {

        // ❌ note ممنوع
        if (request.getNote() != null && !request.getNote().isBlank()) {
            throw new BusinessRuleViolationException(
                    "Note is not allowed for purchase items"
            );
        }

        // ❌ كمية عشرية ممنوعة
        if (request.getQuantity().scale() > 0) {
            throw new BusinessRuleViolationException(
                    "Quantity for purchase items must be an integer number"
            );
        }

        // ❌ صفر أو سالب (احتياط إضافي)
        if (request.getQuantity().compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessRuleViolationException(
                    "Quantity for purchase items must be at least 1"
            );
        }

        // ❌ تكرار منتج شراء
        boolean exists = itemRepo.findByOrderId(order.getId())
                .stream()
                .anyMatch(i -> i.getProductId().equals(product.getId()));

        if (exists) {
            throw new BusinessRuleViolationException(
                    "Purchase product already exists in the order. Update quantity instead."
            );
        }

        return;
    }

    // ===== SERVICE (عصر) =====
    if ("SERVICE".equalsIgnoreCase(product.getProductType())
            || "OLIVE".equalsIgnoreCase(product.getProductType())) {

        if (request.getOliveType() == null || request.getBagsCount() == null) {
            throw new BusinessRuleViolationException(
                    "Service item requires oliveType and bagsCount"
            );
        }

        boolean sameOliveTypeExists = itemRepo.findByOrderId(order.getId())
                .stream()
                .anyMatch(i ->
                        ("SERVICE".equalsIgnoreCase(i.getProductType())
                                || "OLIVE".equalsIgnoreCase(i.getProductType()))
                                && i.getOliveType() != null
                                && i.getOliveType().equalsIgnoreCase(request.getOliveType())
                );

        if (sameOliveTypeExists) {
            throw new BusinessRuleViolationException(
                    "Service item with the same oliveType already exists. Update quantity instead."
            );
        }
    }
}

}

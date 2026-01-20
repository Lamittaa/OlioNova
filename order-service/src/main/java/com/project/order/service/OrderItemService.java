package com.project.order.service;

import com.project.order.client.CustomerClient;
import com.project.order.dto.*;
import com.project.order.exception.OrderNotEditableException;
import com.project.order.exception.ResourceNotFoundException;
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
    private final OrderMapper orderMapper;

    // ========================= READ =========================

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getItems(Long orderId) {
        ensureOrderExists(orderId);
        return itemRepo.findByOrderId(orderId)
                .stream()
                .map(orderMapper::toOrderItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getItem(Long orderId, Long itemId) {
        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                ));
        return orderMapper.toOrderItemResponse(item);
    }

    // ========================= ADD =========================

    public OrderResponse addItem(Long orderId, @org.springframework.validation.annotation.Validated AddOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        boolean isMember = customerClient.getMembership(order.getCustomerId()).isMembership();

        ProductLookup product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        OrderItem item = new OrderItem();
        item.setOrder(order);

        // Snapshot
        item.setProductId(product.getId());
        item.setProductName(product.getProductName());
        item.setProductType(product.getProductType());

        validateQty(request.getQuantity());
        item.setQuantity(request.getQuantity());

        item.setOliveType(request.getOliveType());
        item.setBagsCount(request.getBagsCount());
        item.setNote(request.getNote());

        BigDecimal unitPrice = computeFinalPrice(product, isMember);
        item.setPrice(unitPrice);

        item.setTotalPrice(unitPrice.multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP));

        item.setCreatedAt(LocalDateTime.now());

        itemRepo.save(item);

        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // ========================= UPDATE =========================

    public OrderResponse updateItem(Long orderId, Long itemId, UpdateOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                ));

        // update fields (partial)
        if (request.getQuantity() != null) {
            validateQty(request.getQuantity());
            item.setQuantity(request.getQuantity());
        }
        if (request.getOliveType() != null) item.setOliveType(request.getOliveType());
        if (request.getBagsCount() != null) item.setBagsCount(request.getBagsCount());
        if (request.getNote() != null) item.setNote(request.getNote());

        // recalc totals (unit price snapshot remains same unless you choose to recalc on update)
        item.setTotalPrice(item.getPrice().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP));
        item.setUpdatedAt(LocalDateTime.now());

        itemRepo.save(item);

        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // ========================= DELETE =========================

    public void deleteItem(Long orderId, Long itemId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId=" + orderId + ", itemId=" + itemId
                ));

        itemRepo.delete(item);

        recomputeOrderTotal(order);
    }

    // ========================= Helpers =========================

    private void ensureOrderExists(Long orderId) {
        if (!orderRepo.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }
    }

    private void ensureOrderEditable(Order order) {
        // حسب جدول OrderStatus عندك: Submitted / In Production / Done / Paid / Cancelled / Refunded
        String st = order.getStatus().getStatusName();

        // المسموح تعديل items فقط في Submitted (وإذا بدك كمان In Production غيري الشرط)
        if (!"SUBMITTED".equalsIgnoreCase(st)) {
            throw new OrderNotEditableException("Order is not editable in status: " + st);
        }
    }

    private void validateQty(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        // optional: limit decimals
        if (qty.scale() > 2) {
            throw new IllegalArgumentException("Quantity can have at most 2 decimals");
        }
    }

    private BigDecimal computeFinalPrice(ProductLookup product, boolean isMember) {
        // السعر النهائي للوحدة
        BigDecimal base = product.getPrice(); // BigDecimal
        if (isMember) {
            // memberDiscount عندك مثلا 0.10 => خصم 10%
            BigDecimal discount = product.getMemberDiscount() == null ? BigDecimal.ZERO : product.getMemberDiscount();
            BigDecimal factor = BigDecimal.ONE.subtract(discount);
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
}

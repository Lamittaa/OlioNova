package com.project.order.service;

import com.project.order.client.CustomerClient;
import com.project.order.dto.*;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.OrderMapper;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import com.project.order.model.ProductLookup;
import com.project.order.repository.OrderRepo;
import com.project.order.repository.OrderStatusRepo;
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
public class OrderService {

    private final OrderRepo orderRepo;
    private final ProductLookupRepo productRepo;
    private final OrderStatusRepo statusRepo;

    private final CustomerClient customerClient;
    private final OrderMapper orderMapper;

    // ===================== 1) CREATE ORDER =====================
    // POST /api/orders
    public OrderResponse createOrder(CreateOrderRequest request) {

        boolean isMember = fetchMembership(request.getCustomerId());

        OrderStatus submitted = getStatusOrThrow("SUBMITTED");

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus(submitted);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        for (CreateOrderItemRequest dto : request.getItems()) {
            OrderItem item = buildOrderItem(order, dto, isMember);
            order.getItems().add(item);
        }

        recalcOrderTotal(order);

        Order saved = orderRepo.save(order);
        return orderMapper.toOrderResponse(saved);
    }

    // ===================== 2) GET ORDER BY ID =====================
    // GET /api/orders/{id}
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toOrderResponse(order);
    }

    // ===================== 3) GET ORDERS BY CUSTOMER =====================
    // GET /api/orders?customerId=1
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepo.findByCustomerId(customerId)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    // ===================== 4) ADD ITEM =====================
    // POST /api/orders/{id}/items
    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        boolean isMember = fetchMembership(order.getCustomerId());

        // reuse same builder (بس AddOrderItemRequest نفس fields المطلوبة)
        CreateOrderItemRequest dto = new CreateOrderItemRequest();
        dto.setProductId(request.getProductId());
        dto.setQuantity(request.getQuantity());
        dto.setOliveType(request.getOliveType());
        dto.setBagsCount(request.getBagsCount());
        dto.setNote(request.getNote());

        OrderItem item = buildOrderItem(order, dto, isMember);
        order.getItems().add(item);

        recalcOrderTotal(order);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepo.save(order);
        return orderMapper.toOrderResponse(saved);
    }

    // ===================== 5) UPDATE ITEM =====================
    // PUT /api/orders/{id}/items/{itemId}
    public OrderResponse updateItem(Long orderId, Long itemId, UpdateOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + itemId));

        orderMapper.updateOrderItemFromDto(request, item);

        validateQty(item.getQuantity());
        item.setTotalPrice(item.getPrice().multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP));
        item.setUpdatedAt(LocalDateTime.now());

        recalcOrderTotal(order);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepo.save(order);
        return orderMapper.toOrderResponse(saved);
    }

    // ===================== 6) DELETE ITEM =====================
    // DELETE /api/orders/{id}/items/{itemId}
    public void deleteItem(Long orderId, Long itemId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        ensureOrderEditable(order);

        boolean removed = order.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("OrderItem not found with id: " + itemId);
        }

        recalcOrderTotal(order);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);
    }

    // ===================== 7) UPDATE STATUS =====================
    // PATCH /api/orders/{id}/status
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus newStatus = getStatusOrThrow(request.getStatus());

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepo.save(order);
        return orderMapper.toOrderResponse(saved);
    }

    // ===================== Helpers =====================

    private boolean fetchMembership(Long customerId) {
        var res = customerClient.getMembership(customerId);
        return res != null && res.isMembership(); // ✅ لأنك تستخدم record
    }

    private OrderStatus getStatusOrThrow(String statusName) {
        return statusRepo.findByStatusNameIgnoreCase(statusName)
                .orElseThrow(() -> new ResourceNotFoundException("OrderStatus not found: " + statusName));
    }

    private OrderItem buildOrderItem(Order order, CreateOrderItemRequest dto, boolean isMember) {

        ProductLookup product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + dto.getProductId()));

        OrderItem item = new OrderItem();
        item.setOrder(order);

        // ✅ Snapshot
        item.setProductId(product.getId());
        item.setProductName(product.getProductName());
        item.setProductType(product.getProductType());

        validateQty(dto.getQuantity());
        item.setQuantity(dto.getQuantity());

        item.setOliveType(dto.getOliveType());
        item.setBagsCount(dto.getBagsCount());
        item.setNote(dto.getNote());

        BigDecimal finalPrice = computeFinalPrice(product, isMember);
        item.setPrice(finalPrice);

        item.setTotalPrice(finalPrice.multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP));
        item.setCreatedAt(LocalDateTime.now());

        return item;
    }

    private BigDecimal computeFinalPrice(ProductLookup product, boolean isMember) {

        BigDecimal base = product.getPrice();
        if (base == null) {
            throw new IllegalArgumentException("Product price is missing");
        }

        if (!isMember) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }

        // member discount نسبة (0..1)
        BigDecimal discount = product.getMemberDiscount();
        if (discount == null) {
            return base.setScale(2, RoundingMode.HALF_UP);
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Member discount must be between 0 and 1");
        }

        return base.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalcOrderTotal(Order order) {
        BigDecimal sum = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(sum.setScale(2, RoundingMode.HALF_UP));
    }

    private void validateQty(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    private void ensureOrderEditable(Order order) {
        if (order.getStatus() == null || order.getStatus().getStatusName() == null) return;

        String s = order.getStatus().getStatusName();
        if ("Paid".equalsIgnoreCase(s) || "Cancelled".equalsIgnoreCase(s)) {
            throw new IllegalArgumentException("Order is not editable in status: " + s);
        }
    }
}

package com.project.order.service;

import com.project.order.client.CustomerClient;
import com.project.order.dto.*;
import com.project.order.dto.FulfillResponse;
import com.project.order.exception.*;
import com.project.order.mapper.OrderItemMapper;
import com.project.order.mapper.OrderMapper;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import com.project.order.model.ProductLookup;
import com.project.order.repository.OrderItemRepo;
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
public class OrderItemService {

    private static final String MEMBER_OLIVE_PRODUCT = "زيتون للمساهم";
    private static final String STANDARD_OLIVE_PRODUCT = "زيتون لغير المساهم";

    private final OrderRepo         orderRepo;
    private final OrderItemRepo     itemRepo;
    private final ProductLookupRepo productRepo;
    private final CustomerClient    customerClient;
    private final OrderStatusRepo   statusRepo;
    private final OrderItemMapper   orderItemMapper;
    private final OrderMapper       orderMapper;

    // =====================================================
    // GET ALL ITEMS
    // =====================================================
    @Transactional(readOnly = true)
    public List<OrderItemResponse> getItems(Long orderId) {

        ensureOrderExists(orderId);

        return itemRepo.findByOrderId(orderId)
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    // =====================================================
    // GET SINGLE ITEM
    // =====================================================
    @Transactional(readOnly = true)
    public OrderItemResponse getItem(Long orderId, Long itemId) {

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId="
                        + orderId + ", itemId=" + itemId));

        return orderItemMapper.toResponse(item);
    }

    // =====================================================
    // ADD ITEM
    // =====================================================
    public OrderResponse addItem(Long orderId,
                                  AddOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        ensureOrderEditable(order);

        ProductLookup product =
                productRepo.findById(request.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with id: "
                                + request.getProductId()));

        boolean isMember = customerClient
                .getMembership(order.getCustomerId())
                .isMembership();
        product = resolveCustomerPricedProduct(product, isMember);

        validateAddItemRules(order, product, request);

        OrderItem item = new OrderItem();
        item.setOrder(order);
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
                         .setScale(2, RoundingMode.HALF_UP));

        item.setCreatedAt(LocalDateTime.now());
        item.setStatus(getStatusOrThrow("SUBMITTED"));

        itemRepo.save(item);
        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // =====================================================
    // UPDATE ITEM
    // =====================================================
    public OrderResponse updateItem(Long orderId,
                                     Long itemId,
                                     UpdateOrderItemRequest request) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        ensureOrderEditable(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId="
                        + orderId + ", itemId=" + itemId));

    if (request.getQuantity() != null) {

    if ("PURCHASE".equalsIgnoreCase(item.getProductType())) {

        ProductLookup product =
                productRepo.findById(item.getProductId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product not found"));

        int requested = request.getQuantity().intValue();

        if (product.getInventoryAvailabilityQuantity() == null
                || product.getInventoryAvailabilityQuantity() < requested) {
            throw new OutOfStockException(
                    "Not enough stock for: "
                    + product.getProductName()
                    + ". Available: "
                    + product.getInventoryAvailabilityQuantity()
                    + ", Requested: " + requested);
        }
    }

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
                    .setScale(2, RoundingMode.HALF_UP));

        item.setUpdatedAt(LocalDateTime.now());

        itemRepo.save(item);
        recomputeOrderTotal(order);

        return orderMapper.toOrderResponse(order);
    }

    // =====================================================
    // DELETE ITEM
    // =====================================================
    public void deleteItem(Long orderId, Long itemId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        ensureOrderInSubmittedOnly(order);

        OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found. orderId="
                        + orderId + ", itemId=" + itemId));

        itemRepo.delete(item);
        recomputeOrderTotal(order);
    }

    // =====================================================
    // UPDATE ORDER ITEM STATUS
    // =====================================================
    public void updateOrderItemStatus(Long orderItemId,
                                       String newStatusName) {

        OrderItem item = itemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OrderItem not found"));

        OrderStatus newStatus = getStatusOrThrow(newStatusName);

        validateItemTransition(item, newStatusName);

        item.setStatus(newStatus);
        item.setUpdatedAt(LocalDateTime.now());
        itemRepo.save(item);

        recalculateOrderStatus(item.getOrder().getId());
    }

private void validateAddItemRules(Order order,
                                   ProductLookup product,
                                   AddOrderItemRequest request) {

    if (isStockTrackedPurchase(product)) {

        if (request.getNote() != null
                && !request.getNote().isBlank()) {
            throw new BusinessRuleViolationException(
                    "Note is not allowed for purchase items");
        }

        if (request.getQuantity().scale() > 0) {
            throw new BusinessRuleViolationException(
                    "Quantity for purchase items must be an integer");
        }

        if (request.getQuantity()
                .compareTo(BigDecimal.ONE) < 0) {
            throw new BusinessRuleViolationException(
                    "Quantity must be at least 1");
        }

        // ✅ شيك على المخزون لكل PURCHASE
        if (product.getInventoryAvailabilityQuantity() == null
                || product.getInventoryAvailabilityQuantity() <= 0) {
            throw new OutOfStockException(
                    "Product out of stock: "
                    + product.getProductName());
        }

        int requested = request.getQuantity().intValue();

        if (product.getInventoryAvailabilityQuantity() < requested) {
            throw new OutOfStockException(
                    "Not enough stock for: "
                    + product.getProductName()
                    + ". Available: " + product.getInventoryAvailabilityQuantity()
                    + ", Requested: " + requested);
        }

        boolean exists = itemRepo.findByOrderId(order.getId())
                .stream()
                .anyMatch(i -> i.getProductId()
                        .equals(product.getId()));

        if (exists) {
            throw new BusinessRuleViolationException(
                    "Purchase product already exists in the order."
                    + " Update quantity instead.");
        }

        return;
    }

    // SERVICE
    if (isOliveProduct(product)) {

        if (request.getOliveType() == null
                || request.getBagsCount() == null) {
            throw new BusinessRuleViolationException(
                    "Service item requires oliveType and bagsCount");
        }

        boolean sameOliveTypeExists =
                itemRepo.findByOrderId(order.getId())
                        .stream()
                        .anyMatch(i ->
                                ("SERVICE".equalsIgnoreCase(
                                        i.getProductType())
                                 || "OLIVE".equalsIgnoreCase(
                                        i.getProductType()))
                                && i.getOliveType() != null
                                && i.getOliveType()
                                        .equalsIgnoreCase(
                                                request.getOliveType()));

        if (sameOliveTypeExists) {
            throw new BusinessRuleViolationException(
                    "Service item with same oliveType already exists."
                    + " Update quantity instead.");
        }
    }
}

    // =====================================================
    // HELPERS
    // =====================================================
    private void ensureOrderExists(Long orderId) {
        if (!orderRepo.existsById(orderId)) {
            throw new ResourceNotFoundException(
                    "Order not found with id: " + orderId);
        }
    }

    private void ensureOrderEditable(Order order) {
        String status = order.getStatus()
                .getStatusName().toUpperCase();
        if (!status.equals("SUBMITTED")) {
            throw new OrderNotEditableException(
                    "Order items cannot be modified in status: "
                    + status);
        }
    }

    private void ensureOrderInSubmittedOnly(Order order) {
        String status = order.getStatus()
                .getStatusName().toUpperCase();
        if (!status.equals("SUBMITTED")) {
            throw new OrderNotEditableException(
                    "Order items can be deleted only in SUBMITTED status."
                    + " Current: " + status);
        }
    }

    private void validateQuantity(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0");
        }
        if (qty.scale() > 2) {
            throw new IllegalArgumentException(
                    "Quantity can have at most 2 decimal places");
        }
    }

    private BigDecimal computeFinalPrice(ProductLookup product,
                                          boolean isMember) {
        return product.getPrice().setScale(2, RoundingMode.HALF_UP);
    }

    private ProductLookup resolveCustomerPricedProduct(ProductLookup product,
                                                       boolean isMember) {
        if (!isOliveProduct(product)) {
            return product;
        }

        String productName = isMember ? MEMBER_OLIVE_PRODUCT : STANDARD_OLIVE_PRODUCT;
        return productRepo.findByProductNameIgnoreCaseAndActiveTrue(productName)
                .orElse(product);
    }

    private boolean isOliveProduct(ProductLookup product) {
        return product.getProductType() != null
                && ("OLIVE".equalsIgnoreCase(product.getProductType())
                || "SERVICE".equalsIgnoreCase(product.getProductType()));
    }

    private boolean isStockTrackedPurchase(ProductLookup product) {
        return product.getProductType() != null
                && "PURCHASE".equalsIgnoreCase(product.getProductType());
    }

    private void recomputeOrderTotal(Order order) {

        BigDecimal total = itemRepo.findByOrderId(order.getId())
                .stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(
                total.setScale(2, RoundingMode.HALF_UP));
        order.setUpdatedAt(LocalDateTime.now());

        orderRepo.save(order);
    }

    private OrderStatus getStatusOrThrow(String statusName) {
        return statusRepo.findByStatusNameIgnoreCase(statusName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OrderStatus not found: " + statusName));
    }

    private void validateItemTransition(OrderItem item,
                                        String nextStatus) {
        String current = item.getStatus()
                .getStatusName().toUpperCase();
        String next = nextStatus.toUpperCase();
        String type = item.getProductType().toUpperCase();

        if (current.equalsIgnoreCase(next)) return;

        if (type.equals("PURCHASE")) {
            if (current.equals("PAID") && next.equals("REFUNDED")) return;
            if (current.equals("PAID") && next.equals("COMPLETED")) return;
            throw new BusinessRuleViolationException(
                    "Invalid status change for PURCHASE item");
        }

        if (type.equals("SERVICE")) {
            if (current.equals("PAID") && next.equals("REFUNDED")) return;
            if (current.equals("PAID") && next.equals("IN_PROGRESS")) return;
            if (current.equals("PAID") && next.equals("IN_PRODUCTION")) return;
            if (current.equals("IN_PRODUCTION") && next.equals("IN_PROGRESS")) return;
            if (current.equals("IN_PROGRESS") && next.equals("READY_FOR_PICKUP")) return;
            if (current.equals("READY_FOR_PICKUP") && next.equals("COMPLETED")) return;
            throw new BusinessRuleViolationException(
                    "Invalid status change for SERVICE item");
        }
    }

    private void recalculateOrderStatus(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> items = itemRepo.findByOrderId(orderId);

        if (items.isEmpty()) {
            order.setStatus(getStatusOrThrow("SUBMITTED"));
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
            return;
        }

        int total = items.size();

        int refundedCount = 0;
        int completedCount = 0;

        boolean anyInProgress = false;
        boolean anyInProduction = false;

        boolean allPaid = true;

        boolean hasServiceItems = false;
        boolean allServiceReady = true;

        for (OrderItem i : items) {
            String status = i.getStatus().getStatusName();

            // counts
            if ("REFUNDED".equalsIgnoreCase(status)) refundedCount++;
            if ("COMPLETED".equalsIgnoreCase(status)) completedCount++;

            // dominant states
            if ("IN_PROGRESS".equalsIgnoreCase(status)) anyInProgress = true;
            if ("IN_PRODUCTION".equalsIgnoreCase(status)) anyInProduction = true;

            // all conditions
            if (!"PAID".equalsIgnoreCase(status)) allPaid = false;

            // service logic
            if ("SERVICE".equalsIgnoreCase(i.getProductType())) {
                hasServiceItems = true;
                if (!"READY_FOR_PICKUP".equalsIgnoreCase(status)) {
                    allServiceReady = false;
                }
            }
        }

        // priority resolution
        if (refundedCount == total) {
            order.setStatus(getStatusOrThrow("REFUNDED"));
        } else if (anyInProgress) {
            order.setStatus(getStatusOrThrow("IN_PROGRESS"));
        } else if (anyInProduction) {
            order.setStatus(getStatusOrThrow("IN_PRODUCTION"));
        } else if (hasServiceItems && allServiceReady) {
            order.setStatus(getStatusOrThrow("READY_FOR_PICKUP"));
        } else if (completedCount == total) {
            order.setStatus(getStatusOrThrow("COMPLETED"));
        } else if (allPaid) {
            order.setStatus(getStatusOrThrow("PAID"));
        } else {
            order.setStatus(getStatusOrThrow("SUBMITTED"));
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

public List<OrderItemBulkResponse> getItemsByIds(List<Long> ids) {

    return itemRepo.findAllByIdIn(ids)
            .stream()
            .map(orderItemMapper::toBulkResponse)
            .toList();
}
// =====================================================
// FULFILL ITEM — استلام الشراء
// =====================================================
public FulfillResponse fulfillItem(Long orderId, Long itemId) {

    Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Order not found with id: " + orderId));

    OrderItem item = itemRepo.findByIdAndOrderId(itemId, orderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Order item not found. orderId="
                    + orderId + ", itemId=" + itemId));

    if (!"PURCHASE".equalsIgnoreCase(item.getProductType())) {
        throw new BusinessRuleViolationException(
                "Only PURCHASE items can be fulfilled");
    }

    if (!"PAID".equalsIgnoreCase(item.getStatus().getStatusName())) {
        throw new BusinessRuleViolationException(
                "Item must be in PAID status to fulfill. Current: "
                + item.getStatus().getStatusName());
    }

    ProductLookup product = productRepo.findById(item.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Product not found"));

    // ✅ ينقص من التوتال
    int qty = item.getQuantity().intValue();

    if (product.getInventoryTotalQuantity() == null
            || product.getInventoryTotalQuantity() < qty) {
        throw new OutOfStockException(
                "Not enough total stock to fulfill: "
                + product.getProductName()
                + ". Total: " + product.getInventoryTotalQuantity()
                + ", Requested: " + qty);
    }

    product.setInventoryTotalQuantity(
            product.getInventoryTotalQuantity() - qty);
    productRepo.save(product);

    item.setStatus(getStatusOrThrow("COMPLETED"));
    item.setUpdatedAt(LocalDateTime.now());
    itemRepo.save(item);

    recalculateOrderStatus(orderId);

    FulfillResponse response = new FulfillResponse();
    response.setOrderId(orderId);
    response.setItemId(itemId);
    response.setProductName(item.getProductName());
    response.setProductType(item.getProductType());
    response.setQuantity(item.getQuantity().intValue());
    response.setStatus(item.getStatus().getStatusName());

    return response;
}

public List<String> getPossibleStatuses() {
    return statusRepo.findAll()
            .stream()
            .map(OrderStatus::getStatusName)
            .toList();
}
}

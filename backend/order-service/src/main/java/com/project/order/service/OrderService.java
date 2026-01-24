package com.project.order.service;

import com.project.order.client.CustomerClient;
import com.project.order.dto.CreateOrderItemRequest;
import com.project.order.dto.CreateOrderRequest;
import com.project.order.dto.OrderResponse;
import com.project.order.exception.InvalidOrderItemsException;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.OrderMapper;
import com.project.order.model.Order;
import com.project.order.model.OrderItem;
import com.project.order.model.OrderStatus;
import com.project.order.model.ProductLookup;
import com.project.order.repository.OrderRepo;
import com.project.order.repository.OrderStatusRepo;
import com.project.order.repository.ProductLookupRepo;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepo orderRepo;
    private final ProductLookupRepo productRepo;
    private final OrderStatusRepo statusRepo;
    private final CustomerClient customerClient;
    private final OrderMapper orderMapper;

    // ================= CREATE =================
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateOrderItems(request.getItems());

        boolean isMember = fetchMembership(request.getCustomerId());
        OrderStatus submitted = getStatus("SUBMITTED");

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus(submitted);
        order.setCreatedAt(LocalDateTime.now());

        for (CreateOrderItemRequest dto : request.getItems()) {
            order.getItems().add(buildItem(order, dto, isMember));
        }

        recalcTotal(order);

        return buildOrderResponse(orderRepo.save(order));
    }

    // ================= READ =================
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {

        Order order = orderRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id));

        return buildOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {

        return orderRepo.findByCustomerId(customerId)
                .stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    // ================= CANCEL =================
    public void cancelOrder(Long id) {

        Order order = orderRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(getStatus("CANCELED"));
        order.setUpdatedAt(LocalDateTime.now());
    }

    // ================= RESPONSE BUILDER =================
    private OrderResponse buildOrderResponse(Order order) {

        OrderResponse response = orderMapper.toOrderResponse(order);

        response.setItems(
                order.getItems()
                        .stream()
                        .map(orderMapper::mapItem)
                        .toList()
        );

        return response;
    }

    // ================= HELPERS =================
    private void recalcTotal(Order order) {
        order.setTotalPrice(
                order.getItems().stream()
                        .map(OrderItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal computePrice(ProductLookup product, boolean isMember) {
        if (!isMember || product.getMemberDiscount() == null) {
            return product.getPrice();
        }
        return product.getPrice()
                .multiply(BigDecimal.ONE.subtract(product.getMemberDiscount()));
    }

    private OrderStatus getStatus(String status) {
        return statusRepo.findByStatusNameIgnoreCase(status)
                .orElseThrow(() ->
                        new ResourceNotFoundException("OrderStatus not found: " + status));
    }

    private boolean fetchMembership(Long customerId) {
        try {
            return customerClient.getMembership(customerId).isMembership();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(
                    "Customer not found with id: " + customerId);
        }
    }

    // ================= VALIDATION =================
    private void validateOrderItems(List<CreateOrderItemRequest> items) {

        if (items == null || items.isEmpty()) {
            throw new InvalidOrderItemsException("Order must contain at least one item");
        }

        Set<Long> purchaseProducts = new HashSet<>();
        Set<String> oliveServices = new HashSet<>();

        for (CreateOrderItemRequest item : items) {

            ProductLookup product = productRepo.findById(item.getProductId())
                    .orElseThrow(() ->
                            new InvalidOrderItemsException("Invalid productId"));

            if ("SERVICE".equals(product.getProductType())) {

                if (item.getOliveType() == null || item.getBagsCount() == null) {
                    throw new InvalidOrderItemsException("Service requires oliveType & bagsCount");
                }

                String key = product.getId() + "|" + item.getOliveType();
                if (!oliveServices.add(key)) {
                    throw new InvalidOrderItemsException("Duplicate service with same oliveType");
                }

            } else {

                if (!purchaseProducts.add(product.getId())) {
                    throw new InvalidOrderItemsException("Duplicate purchase product");
                }
            }
        }
    }

    // ================= ITEM BUILDER =================
    private OrderItem buildItem(Order order,
                                CreateOrderItemRequest dto,
                                boolean isMember) {

        ProductLookup product = productRepo.findById(dto.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(product.getId());
        item.setProductName(product.getProductName());
        item.setProductType(product.getProductType());
        item.setQuantity(dto.getQuantity());
        item.setOliveType(dto.getOliveType());
        item.setBagsCount(dto.getBagsCount());
        item.setNote(dto.getNote());

        BigDecimal price = computePrice(product, isMember);
        item.setPrice(price);
        item.setTotalPrice(price.multiply(dto.getQuantity()));

        item.setCreatedAt(LocalDateTime.now());

        return item;
    }

@Transactional(readOnly = true)
public List<OrderResponse> getOrdersByNationalId(String nationalId) {

    Long customerId;

    try {
        customerId = customerClient
                .getByNationalId(nationalId)
                .getId();
    } catch (FeignException.NotFound e) {
        throw new ResourceNotFoundException(
                "Customer not found with nationalId: " + nationalId
        );
    }

    return orderRepo.findByCustomerId(customerId)
            .stream()
            .map(this::buildOrderResponse)
            .toList();
}


}

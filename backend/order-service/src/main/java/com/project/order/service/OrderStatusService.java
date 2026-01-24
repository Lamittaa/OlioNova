package com.project.order.service;

import com.project.order.dto.OrderResponse;
import com.project.order.dto.OrderStatusResponse;
import com.project.order.dto.UpdateOrderStatusRequest;
import com.project.order.exception.InvalidOrderStatusTransitionException;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.OrderMapper;
import com.project.order.mapper.OrderStatusMapper;
import com.project.order.model.Order;
import com.project.order.model.OrderStatus;
import com.project.order.repository.OrderRepo;
import com.project.order.repository.OrderStatusRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderStatusService {

    private final OrderStatusRepo statusRepo;
    private final OrderStatusMapper statusMapper;
    private final OrderRepo orderRepo;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public OrderStatusResponse getStatusById(Long id) {
        OrderStatus status = statusRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order status not found with id: " + id));
        return statusMapper.toOrderStatusResponse(status);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusResponse> getAllStatuses() {
        return statusRepo.findAll()
                .stream()
                .map(statusMapper::toOrderStatusResponse)
                .toList();
    }
    private OrderStatus getStatusOrThrow(String statusName) {
        return statusRepo.findByStatusNameIgnoreCase(statusName)
                .orElseThrow(() -> new ResourceNotFoundException("OrderStatus not found: " + statusName));
    }
    // ===================== 7) UPDATE STATUS =====================
    // PATCH /api/orders/{id}/status
   public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {

    Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Order not found with id: " + orderId));

    String currentStatus = order.getStatus().getStatusName();
    OrderStatus newStatus = getStatusOrThrow(request.getStatus());
    String nextStatus = newStatus.getStatusName();

    // ⭐ التحقق الأساسي
    validateStatusTransition(currentStatus, nextStatus);

    order.setStatus(newStatus);
    order.setUpdatedAt(LocalDateTime.now());

    return orderMapper.toOrderResponse(orderRepo.save(order));
}

    private void validateStatusTransition(String current, String next) {

    if (current.equals("SUBMITTED") &&
            (next.equals("READY_FOR_PAYMENT") || next.equals("CANCELED"))) return;

    if (current.equals("READY_FOR_PAYMENT") &&
            (next.equals("PAID") || next.equals("CANCELED"))) return;

    if (current.equals("PAID") &&
            (next.equals("IN_PROGRESS") || next.equals("CANCELED"))) return;

    if (current.equals("IN_PROGRESS") &&
            next.equals("COMPLETED")) return;

    throw new InvalidOrderStatusTransitionException(
            "Invalid order status transition: " + current + " → " + next
    );
}

}

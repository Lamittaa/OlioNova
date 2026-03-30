package com.project.order.controller;

import com.project.order.dto.CreateOrderRequest;
import com.project.order.dto.OrderDashboardResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.UpdateOrderStatusRequest;
import com.project.order.service.OrderDashboardService;
import com.project.order.service.OrderService;
import com.project.order.service.OrderStatusService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

        private final OrderStatusService orderStatusService;
        
        private final OrderDashboardService dashboardService;
        private final OrderService orderService;

        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
        public ResponseEntity<OrderResponse> createOrder(
                        @Valid @RequestBody CreateOrderRequest request) {
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(orderService.createOrder(request));
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('ORDER_READ')")
        public ResponseEntity<OrderResponse> getOrderById(
                        @PathVariable @Min(1) Long id) {
                return ResponseEntity.ok(orderService.getOrderById(id));
        }

        @GetMapping
        @PreAuthorize("hasAuthority('ORDER_READ')")
        public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
                        @RequestParam @Min(1) Long customerId) {
                return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
        public ResponseEntity<Void> cancelOrder(
                        @PathVariable @Min(1) Long id) {
                orderService.cancelOrder(id);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/search/by-national-id")
        @PreAuthorize("hasAuthority('ORDER_READ')")
        public ResponseEntity<List<OrderResponse>> searchByNationalId(
                        @RequestParam String nationalId) {
                return ResponseEntity.ok(
                                orderService.getOrdersByNationalId(nationalId));
        }

        @PutMapping("/{id}/status")
        @PreAuthorize("hasAuthority('ORDER_UPDATE_STATUS')")
        public ResponseEntity<OrderResponse> updateOrderStatus(
                        @PathVariable @Min(1) Long id,
                        @Valid @RequestBody UpdateOrderStatusRequest request) {
                return ResponseEntity.ok(
                                orderStatusService.updateStatus(id, request));
        }

        @PostMapping("/bulk")
        public ResponseEntity<List<OrderDashboardResponse>> getOrdersByIds(
                        @RequestBody List<Long> ids) {
                return ResponseEntity.ok(
                                dashboardService.getOrdersByIds(ids));
        }

        @PostMapping("/{id}/pay")
@PreAuthorize("hasRole('ADMIN') ")
public ResponseEntity<Void> payOrder(
        @PathVariable @Min(1) Long id) {
    orderService.payOrder(id);
    return ResponseEntity.noContent().build();
}
}

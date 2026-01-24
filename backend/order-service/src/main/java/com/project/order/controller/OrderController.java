package com.project.order.controller;

import com.project.order.dto.CreateOrderRequest;
import com.project.order.dto.OrderResponse;
import com.project.order.service.OrderService;
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

    private final OrderService orderService;

    // 1️⃣ Create Order
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }

    // 2️⃣ Get Order By ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable @Min(1) Long id
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // 3️⃣ Get Orders By Customer
    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ')")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @RequestParam @Min(1) Long customerId
    ) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    // 4️⃣ Cancel Order (Soft Delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable @Min(1) Long id
    ) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }


@GetMapping("/search/by-national-id")
@PreAuthorize("hasAuthority('ORDER_READ')")
public ResponseEntity<List<OrderResponse>> searchByNationalId(
        @RequestParam String nationalId
) {
    return ResponseEntity.ok(
            orderService.getOrdersByNationalId(nationalId)
    );
}


}

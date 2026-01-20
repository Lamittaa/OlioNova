package com.project.order.controller;

import com.project.order.dto.CreateOrderRequest;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.UpdateOrderStatusRequest;
import com.project.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Create and manage orders (read + status).")
public class OrderController {

    private final OrderService orderService;

    // ================= CREATE ORDER =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('ORDER_CREATE')")
    @Operation(
            summary = "Create order",
            description = "Creates a new order with one or more items. Prices are computed using ProductLookup + membership."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error / invalid payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_CREATE)"),
            @ApiResponse(responseCode = "404", description = "Customer/Product not found")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    // ================= GET ORDER BY ID =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_READ')")
    @Operation(summary = "Get order by ID", description = "Returns a single order including its items.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID (must be positive)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_READ)"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable @Min(value = 1, message = "Order ID must be greater than 0") Long id
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ================= GET ORDERS BY CUSTOMER =================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_READ')")
    @Operation(summary = "Get orders by customer", description = "Returns all orders for a specific customer (query param).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders returned successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid customerId"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_READ)")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @RequestParam("customerId") @Min(value = 1, message = "Customer ID must be greater than 0") Long customerId
    ) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    // ================= UPDATE STATUS =================
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_UPDATE_STATUS')")
    @Operation(
            summary = "Update order status",
            description = "Updates order status (e.g., SUBMITTED -> READY_FOR_PAYMENT -> PAID -> IN_PROGRESS -> COMPLETED)."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Status update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateOrderStatusRequest",
                            value = """
                            { "status": "READY_FOR_PAYMENT" }
                            """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_UPDATE_STATUS)"),
            @ApiResponse(responseCode = "404", description = "Order/Status not found")
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable @Min(value = 1, message = "Order ID must be greater than 0") Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}

package com.project.order.controller;

import com.project.order.dto.OrderStatusResponse;
import com.project.order.service.OrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-statuses")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Statuses", description = "Lookup table for order statuses (read-only).")
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_STATUS_READ')")
    @Operation(summary = "Get order status by ID", description = "Returns a single order status by its ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order status found",
                    content = @Content(
                            schema = @Schema(implementation = OrderStatusResponse.class),
                            examples = @ExampleObject(
                                    name = "OrderStatus",
                                    value = """
                                    {
                                      "id": 1,
                                      "statusName": "SUBMITTED"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid ID (must be positive)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_STATUS_READ)"),
            @ApiResponse(responseCode = "404", description = "Order status not found")
    })
    public ResponseEntity<OrderStatusResponse> getStatusById(
            @PathVariable @Min(value = 1, message = "Status ID must be greater than 0") Long id
    ) {
        return ResponseEntity.ok(orderStatusService.getStatusById(id));
    }

    // ================= GET ALL (id + name) =================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_STATUS_READ')")
    @Operation(summary = "Get all order statuses", description = "Returns all order statuses (id + name).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statuses returned successfully",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = OrderStatusResponse.class)),
                            examples = @ExampleObject(
                                    name = "OrderStatusList",
                                    value = """
                                    [
                                      {"id": 1, "statusName": "SUBMITTED"},
                                      {"id": 3, "statusName": "PAID"},
                                      {"id": 4, "statusName": "IN_PROGRESS"},
                                      {"id": 5, "statusName": "COMPLETED"},
                                      {"id": 6, "statusName": "CANCELED"}
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_STATUS_READ)")
    })
    public ResponseEntity<List<OrderStatusResponse>> getAllStatuses() {
        return ResponseEntity.ok(orderStatusService.getAllStatuses());
    }

    // ================= GET VALUES ONLY =================
    // ✅ Frontend dropdown helper
    @GetMapping("/values")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','ACCOUNTANT','TECHNICIAN') and hasAuthority('ORDER_STATUS_READ')")
    @Operation(summary = "Get order status values", description = "Returns status names only (for dropdowns).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status names returned successfully",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = String.class)),
                            examples = @ExampleObject(
                                    name = "StatusValues",
                                    value = """
                                    [
                                      "SUBMITTED",
                                      "PAID",
                                      "IN_PROGRESS",
                                      "COMPLETED",
                                      "CANCELED"
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden (missing ORDER_STATUS_READ)")
    })
    public ResponseEntity<List<String>> getStatusValues() {
        return ResponseEntity.ok(
                orderStatusService.getAllStatuses()
                        .stream()
                        .map(OrderStatusResponse::getStatusName)
                        .toList()
        );
    }


}

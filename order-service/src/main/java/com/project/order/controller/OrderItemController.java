package com.project.order.controller;

import com.project.order.dto.AddOrderItemRequest;
import com.project.order.dto.OrderItemResponse;
import com.project.order.dto.OrderResponse;
import com.project.order.dto.UpdateOrderItemRequest;
import com.project.order.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/items")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Items", description = "Manage items inside an order")
public class OrderItemController {

    private final OrderItemService orderItemService;

    // ======================= GET ALL ITEMS =======================

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_ITEM_READ')")
    @Operation(summary = "Get order items", description = "Returns all items for a given order")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Items returned successfully",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class)),
                            examples = @ExampleObject(
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "productId": 1,
                                        "productName": "Olive",
                                        "productType": "OLIVE",
                                        "quantity": 120,
                                        "price": 4.00,
                                        "totalPrice": 480.00,
                                        "oliveType": "Green",
                                        "bagsCount": 3,
                                        "note": "First press"
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<List<OrderItemResponse>> getItems(
            @PathVariable @Min(value = 1, message = "Order ID must be greater than 0") Long orderId
    ) {
        return ResponseEntity.ok(orderItemService.getItems(orderId));
    }

    // ======================= GET SINGLE ITEM =======================

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('ORDER_ITEM_READ')")
    @Operation(summary = "Get order item", description = "Returns a single item from an order")
    public ResponseEntity<OrderItemResponse> getItem(
            @PathVariable @Min(1) Long orderId,
            @PathVariable @Min(1) Long itemId
    ) {
        return ResponseEntity.ok(orderItemService.getItem(orderId, itemId));
    }

    // ======================= ADD ITEM =======================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('ORDER_ITEM_ADD')")
    @Operation(
            summary = "Add item to order",
            description = "Adds a new item to an existing order",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "productId": 1,
                                      "quantity": 100,
                                      "oliveType": "Black",
                                      "bagsCount": 4,
                                      "note": "Urgent"
                                    }
                                    """
                            )
                    )
            )
    )
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable @Min(1) Long orderId,
            @Valid @RequestBody AddOrderItemRequest request
    ) {
        return ResponseEntity.ok(orderItemService.addItem(orderId, request));
    }

    // ======================= UPDATE ITEM =======================

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('ORDER_ITEM_UPDATE')")
    @Operation(summary = "Update order item", description = "Updates an existing order item")
    public ResponseEntity<OrderResponse> updateItem(
            @PathVariable @Min(1) Long orderId,
            @PathVariable @Min(1) Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request
    ) {
        return ResponseEntity.ok(orderItemService.updateItem(orderId, itemId, request));
    }

    // ======================= DELETE ITEM =======================

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') and hasAuthority('ORDER_ITEM_DELETE')")
    @Operation(summary = "Delete order item", description = "Deletes an item from an order")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order or item not found")
    })
    public ResponseEntity<Void> deleteItem(
            @PathVariable @Min(1) Long orderId,
            @PathVariable @Min(1) Long itemId
    ) {
        orderItemService.deleteItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }
}

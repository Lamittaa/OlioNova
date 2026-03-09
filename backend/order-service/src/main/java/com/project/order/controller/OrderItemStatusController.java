package com.project.order.controller;

import com.project.order.dto.UpdateOrderStatusRequest;
import com.project.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/items")
@RequiredArgsConstructor
public class OrderItemStatusController {

    private final OrderItemService orderItemService;

    @PutMapping("/{itemId}/status")
    public ResponseEntity<Void> updateItemStatus(
            @PathVariable Long itemId,
            @RequestBody UpdateOrderStatusRequest request) {

        orderItemService.updateOrderItemStatus(
                itemId,
                request.getStatus()
        );

        return ResponseEntity.ok().build();
    }
}
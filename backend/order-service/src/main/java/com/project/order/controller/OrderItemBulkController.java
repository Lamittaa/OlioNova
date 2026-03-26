package com.project.order.controller;
import com.project.order.dto.OrderItemBulkResponse;
import com.project.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders/items") // 🔥 بدون orderId
@RequiredArgsConstructor
public class OrderItemBulkController {

    private final OrderItemService orderItemService;
     

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ORDER_ITEM_READ')")
    public ResponseEntity<List<OrderItemBulkResponse>> getItemsByIds(
            @RequestBody List<Long> itemIds) {

        return ResponseEntity.ok(
                orderItemService.getItemsByIds(itemIds)
        );
    }
}
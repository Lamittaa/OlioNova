package com.project.productionStages.client;

import com.project.productionStages.config.FeignAuthForwardConfig;
import com.project.productionStages.dto.OrderItemStatusResponse;
import com.project.productionStages.dto.OrderResponse;

import com.project.productionStages.dto.OrderItemResponse;
import com.project.productionStages.dto.OrdersDashboardDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "order-service",
        configuration = FeignAuthForwardConfig.class
)
public interface OrderClient {

    @PutMapping("/api/orders/{orderId}/status")
    void updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody Map<String, String> body  // ← غير من RequestParam لـ RequestBody
    );

    @PostMapping("/api/orders/dashboard/bulk")
    List<OrderResponse> getOrdersByIds(
            @RequestBody List<Long> ids
    );

    @GetMapping("/api/orders/{orderId}")
    OrderResponse getOrderById(@PathVariable("orderId") Long orderId);

    default List<OrdersDashboardDto> getOrdersByIdsForProd(List<Long> ids) {

        var orders = getOrdersByIds(ids);
        return orders.stream()
                .map(order -> {
                    List<OrderItemStatusResponse> items = order.getItems().stream()
                            .filter(i -> i.getProductType().equalsIgnoreCase("SERVICE"))
                            .toList();
//                    int itemsCount = items.size();
//
//                    long completedItems = items.stream()
//                            .filter(item ->
//                                    item.getStatus().equalsIgnoreCase("COMPLETED")
//                            )
//                            .count();
                    return OrdersDashboardDto.builder()
                            .orderId(order.getOrderId())
                            .status(order.getStatus())
                            .items(items)
                            .build();
                })
                .toList();
    }


    @PostMapping("/api/orders/items/bulk")
    List<OrderItemResponse> getOrderItemsByIds(
            @RequestBody List<Long> ids
    );

    @PutMapping("/api/orders/items/{orderItemId}/status")
    void updateOrderItemStatus(
            @PathVariable Long orderItemId,
            @RequestBody Map<String, String> status
    );
}

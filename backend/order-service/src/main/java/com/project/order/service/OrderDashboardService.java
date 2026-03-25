package com.project.order.service;

import com.project.order.dto.OrderDashboardResponse;
import com.project.order.dto.OrderItemStatusResponse;
import com.project.order.model.Order;
import com.project.order.repository.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDashboardService {

    private final OrderRepo orderRepo;

    public List<OrderDashboardResponse> getOrdersByIds(List<Long> ids) {

        List<Order> orders = orderRepo.findAllById(ids);

        return orders.stream().map(order -> {

            int itemsCount = order.getItems().size();

            long completedItems = order.getItems().stream()
                    .filter(item ->
                            item.getStatus().getStatusName().equalsIgnoreCase("COMPLETED")
                    )
                    .count();

            List<OrderItemStatusResponse> items = order.getItems().stream()
                    .map(item -> OrderItemStatusResponse.builder()
                            .id(item.getId())
                            .status(item.getStatus().getStatusName())
                            .build())
                    .collect(Collectors.toList());

            return OrderDashboardResponse.builder()
                    .orderId(order.getId())
                    .itemsCount(itemsCount)
                    .completedItems(completedItems)
                    .items(items)
                    .build();

        }).collect(Collectors.toList());
    }
}
package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.*;
import com.project.productionStages.model.DashboardSort;
import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageType;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderManagementService {
    private final QueueClient queueClient;
    private final OrderClient orderClient;
    private final ProductionStageRepository stageRepository;

    public List<ProductionDashboardDto> getDashboardFiltered(String sort, String statusFilter) {

        List<ProductionDashboardDto> dashboardList = getDashboard();

        DashboardSort dashboardSort = Optional.ofNullable(sort)
                .map(DashboardSort::of)
                .orElse(DashboardSort.QUEUE);
        return dashboardList.stream()
                // Filter (optional)
                .filter(s -> statusFilter == null || s.getStatus().equalsIgnoreCase(statusFilter))
                // Sort
                .sorted((a, b) -> switch (dashboardSort) {
                    case QUEUE -> compareNullable(a.getQueueNumber(), b.getQueueNumber());

                    case ID -> compareNullable(a.getOrderId(), b.getOrderId());

                    case STATUS -> compareNullableText(a.getStatus(), b.getStatus());
                })
                .toList();

    }

    public List<ProductionDashboardDto> getDashboard() {
        List<QueueTicketResponse> ticketsResponse;
        try {
            ticketsResponse = queueClient.getTicketsByQueueType("PRODUCTION", LocalDate.now())
                    .stream()
                    .filter(t -> t.getOrderId() != null)
                    .toList();
        } catch (Exception ex) {
            log.warn("Unable to load production queue tickets for dashboard", ex);
            return List.of();
        }

        List<Long> orderIds = ticketsResponse.stream()
                .map(QueueTicketResponse::getOrderId)
                .distinct()
                .toList();

        if (orderIds.isEmpty()) {
            return List.of();
        }

        List<OrdersDashboardDto> orders;
        try {
            orders = orderClient.getOrdersByIdsForProd(orderIds);
        } catch (Exception ex) {
            log.warn("Unable to load orders for production dashboard", ex);
            return List.of();
        }

        Map<Long, QueueTicketResponse> queueMap = ticketsResponse.stream()
                .collect(Collectors.toMap(
                        QueueTicketResponse::getOrderId,
                        t -> t,
                        (t1, t2) -> t1));

        Map<Long, List<Long>> completedItemsByOrder = orders.stream()
                .collect(Collectors.toMap(OrdersDashboardDto::getOrderId, o -> {
                            return o.getItems().stream()
                                    .filter(i -> Stream.of("COMPLETED","READY_FOR_PICKUP").anyMatch(item->item.equals(i.getStatus())))
                                    .map(OrderItemStatusResponse::getId)
                                    .toList();
                        }
                ));
        List<Long> allCompletedItemIds = completedItemsByOrder.values().stream()
                .flatMap(Collection::stream)
                .toList();

        Map<Long, List<ProductionStage>> ordersStages = stageRepository.findByOrderItemIdIn(allCompletedItemIds)
                .stream()
                .collect(Collectors.groupingBy(ProductionStage::getOrderId));

        return orders.stream().map(order -> {

            QueueTicketResponse ticket = queueMap.get(order.getOrderId());

            int itemsCount = order.getItems().size();

            List<Long> completedItemsIds = completedItemsByOrder.getOrDefault(order.getOrderId(), List.of());

            long completedItems = completedItemsIds.size();

            String status = ticket != null && ticket.getTicketStatus() != null
                    ? ticket.getTicketStatus()
                    : order.getStatus();
            return ProductionDashboardDto.builder()
                    .orderId(order.getOrderId())
                    .status(status)
                    .itemsCount(itemsCount)
                    .completedItems(completedItems)
                    .queueNumber(ticket != null ? ticket.getTicketNumber() : null)
                    .storage(Optional.ofNullable(ordersStages.get(order.getOrderId()))
                            .map(stages ->
                                    stages.stream().filter(s -> s.getStageType().equals(StageType.STORAGE))
                                            .map(ProductionStage::getContainer).toList())
                            .orElse(List.of())
                    )
                    .build();

        }).toList();
    }

    private static <T extends Comparable<T>> int compareNullable(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static int compareNullableText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }
}

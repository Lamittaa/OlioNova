package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.*;
import com.project.productionStages.model.DashboardSort;
import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageType;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
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
                    case QUEUE -> a.getQueueNumber().compareTo(b.getQueueNumber());

                    case ID -> a.getOrderId().compareTo(b.getOrderId());

                    case STATUS -> a.getStatus().compareToIgnoreCase(b.getStatus());
                })
                .toList();

    }

    public List<ProductionDashboardDto> getDashboard() {

        List<QueueTicketResponse> ticketsResponse = queueClient.getTicketsByQueueType("PRODUCTION", LocalDate.now())
                .stream()
                .filter(t -> t.getOrderId() != null)
                .toList();
        List<Long> orderIds = ticketsResponse.stream()
                .map(QueueTicketResponse::getOrderId)
                .toList();

        List<OrdersDashboardDto> orders = orderClient.getOrdersByIdsForProd(orderIds);

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

            String status = ticket.getTicketStatus();
            return ProductionDashboardDto.builder()
                    .orderId(order.getOrderId())
                    .status(status)
                    .itemsCount(itemsCount)
                    .completedItems(completedItems)
                    .queueNumber(ticket.getTicketNumber())
                    .storage(Optional.ofNullable(ordersStages.get(order.getOrderId()))
                            .map(stages ->
                                    stages.stream().filter(s -> s.getStageType().equals(StageType.STORAGE))
                                            .map(ProductionStage::getContainer).toList())
                            .orElse(List.of())
                    )
                    .build();

        }).toList();
    }
}
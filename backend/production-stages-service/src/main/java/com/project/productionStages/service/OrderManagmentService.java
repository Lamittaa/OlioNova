package com.project.productionStages.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.project.productionStages.dto.OrderItemStatusResponse;
import org.springframework.stereotype.Service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.OrderDashboardResponse;
import com.project.productionStages.dto.ProductionDashboardDto;
import com.project.productionStages.dto.QueueTicketResponse;
import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageType;
import com.project.productionStages.repository.ProductionStageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderManagmentService {
    private final QueueClient queueClient;
    private final OrderClient orderClient;
    private final ProductionStageRepository stageRepository;

    public List<ProductionDashboardDto> getDashboard() {

        List<QueueTicketResponse> ticketsResponse = queueClient.getTicketsByQueueType("PRODUCTION", LocalDate.now());
        List<Long> orderIds = ticketsResponse.stream()
                .map(QueueTicketResponse::getOrderId)
                .toList();

       List<OrderDashboardResponse> orders = orderClient.getOrdersByIds(orderIds);

        Map<Long, QueueTicketResponse> queueMap = ticketsResponse.stream()
                .collect(Collectors.toMap(
                        QueueTicketResponse::getOrderId,
                        t -> t));

        return orders.stream().map(order -> {

            QueueTicketResponse ticket = queueMap.get(order.getOrderId());

            int itemsCount = order.getItems().size();

            long completedItems = order.getItems().stream()
                    .filter(i -> i.getStatus().equals("COMPLETED"))
                    .count();

            String status = ticket.getTicketStatus();

            List<Long> completedItemsIds = order.getItems().stream()
                    .filter(i -> i.getStatus().equals("COMPLETED"))
                    .map(OrderItemStatusResponse::getId)
                    .toList();

            List<ProductionStage> stages = stageRepository.findByOrderItemIdIn(completedItemsIds);

            return ProductionDashboardDto.builder()
                    .orderId(order.getOrderId())
                    .status(status)
                    .itemsCount(itemsCount)
                    .completedItems(completedItems)
                    .queueNumber(
                            ticket.getTicketStatus() .equals("WAITING") 
                                    ? ticket.getTicketNumber()
                                    : null)
                    .storage(
                            stages.stream()
                                    .filter(s -> s.getStageType() == StageType.STORAGE)
                                    .map(ProductionStage::getContainer)
                                    .toList())

                    .build();

        }).toList();
    }
}
package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.*;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionService {

    private final ProductionStageRepository stageRepository;
    private final QueueClient queueClient;
    private final OrderClient orderClient;

    // =========================================================
    // ✅ 2. Get Order Details
    // =========================================================
    public OrderDetailsResponse getOrderDetails(Long orderId) {

        List<ProductionStage> stages = stageRepository.findByOrderId(orderId);
        List<OrdersDashboardDto> orderResponseList = orderClient.getOrdersByIdsForProd(List.of(orderId));
        OrdersDashboardDto orderResponse;
        if (!orderResponseList.isEmpty()) {
            orderResponse = orderResponseList.getFirst();
        } else {
            throw new ResourceNotFoundException("Order Not Found");
        }
        Map<Long, OrderItemResponse> orderItemMap = orderClient.getOrderItemsByIds(orderResponse.getItems().stream()
                        .map(OrderItemStatusResponse::getId).toList())
                .stream()
                .collect(Collectors.toMap(OrderItemResponse::getOrderItemId, i -> i));

        if (stages.isEmpty()) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }

        Map<Long, List<ProductionStage>> grouped = stages.stream()
                .collect(Collectors.groupingBy(ProductionStage::getOrderItemId));

        List<OrderItemStageResponse> items = new ArrayList<>();

        for (Long itemId : orderItemMap.keySet()) {

            List<ProductionStage> itemStages = grouped.get(itemId);

            var orderItem = orderItemMap.get(itemId);

            var itemResponse = OrderItemStageResponse.builder()
                    .orderItemId(itemId)
                    .itemStatus(orderItem.getItemStatus())
                    .oliveType(orderItem.getOliveType())
                    .weight(orderItem.getWeight())
                    .build();
            ProductionStage current = Optional.ofNullable(itemStages).map(is -> is.stream()
                    .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null)).orElse(null);
            if (current != null) {

                itemResponse.setStageOrder(current.getStageOrder());
                itemResponse.setStageType(current.getStageType().name());
                itemResponse.setLine(current.getLine());
                itemResponse.setContainer(current.getContainer());
            }
            items.add(itemResponse);
        }

        return OrderDetailsResponse.builder()
                .orderId(orderId)
                .status(orderResponse.getStatus())
                .items(items)
                .build();
    }

    public MoveStageResponse changeStage(Long itemId, StageType nextStageType, String nextContainer) {

        ProductionStage current = getCurrentStage(itemId);

        // =========================================================
        // تنظيف الحالي
        // =========================================================
        emptyStage(current);

        Long orderId = current.getOrderId();
        String line = current.getLine();

        ProductionStage next = stageRepository
                .findByLineAndStageTypeAndContainer(line, nextStageType, nextContainer)
                .orElseThrow(() -> new ResourceNotFoundException("Next stage not found"));

        if (next.getCurrentStatus() != StageStatus.EMPTY) {
            throw new BusinessRuleException("Container is occupied", "CONTAINER_OCCUPIED");
        }


        boolean isLast = next.getStageType() == StageType.STORAGE;


        // =========================================================
        // نقل
        // =========================================================
        next.setOrderItemId(itemId);
        next.setOrderId(orderId);
        next.setCurrentStatus(StageStatus.IN_PROGRESS);

        stageRepository.save(next);

        // =========================================================
        // إذا STORAGE → update order
        // =========================================================
        if (isLast) {
            orderClient.updateOrderItemStatus(
                    itemId,
                    Map.of("status", "READY_FOR_PICKUP")
            );
        }

        return MoveStageResponse.builder()
                .isLastStage(isLast)
                .message(isLast ? "Reached final stage" : "Moved to next stage")
                .build();
    }

    private void emptyStage(ProductionStage current) {
        current.setCurrentStatus(StageStatus.EMPTY);
        current.setOrderItemId(null);
        current.setOrderId(null);
        stageRepository.save(current);
    }

    // =========================================================
    // 🔹 Helpers
    // =========================================================

    private ProductionStage getCurrentStage(Long itemId) {

        return stageRepository.findByOrderItemId(itemId).stream()
                .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active stage for item: " + itemId));
    }

    public void startProduction(StartProductionRequest request) {

        Long orderItemId = request.getOrderItemId();
        Long orderId = request.getOrderId();

        if (stageRepository.existsByOrderItemId(orderItemId)) {
            throw new BusinessRuleException("Already in production", "ALREADY_STARTED");
        }

        StageType stageType = resolveStageType(request.getStageType());
        String line = resolveLine(request.getLine());
        String container = resolveContainer(line, stageType, request.getContainer());

        ProductionStage stage = stageRepository
                .findByLineAndStageTypeAndContainer(line, stageType, container)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found"));

        if (stage.getCurrentStatus() != StageStatus.EMPTY) {
            throw new BusinessRuleException(
                    "Container is already in use",
                    "CONTAINER_OCCUPIED"
            );
        }

        stage.setOrderItemId(orderItemId);
        stage.setOrderId(orderId);
        stage.setCurrentStatus(StageStatus.IN_PROGRESS);

        stageRepository.save(stage);

        orderClient.updateOrderStatus(orderId, Map.of("status", "IN_PROGRESS"));
        orderClient.updateOrderItemStatus(orderItemId, Map.of("status", "IN_PROGRESS"));
        queueClient.updateStatusByOrderId(orderId, "PRODUCTION", "SERVING");
    }

    private StageType resolveStageType(String requestedStageType) {
        if (requestedStageType == null || requestedStageType.isBlank()) {
            return StageType.CLEANING;
        }

        try {
            return StageType.valueOf(requestedStageType.toUpperCase());
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid stage type", "INVALID_STAGE");
        }
    }

    private String resolveLine(String requestedLine) {
        if (requestedLine != null && !requestedLine.isBlank()) {
            return requestedLine.trim();
        }

        return stageRepository.findAvailableLines().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No production line is currently available"));
    }

    private String resolveContainer(String line, StageType stageType, String requestedContainer) {
        if (requestedContainer != null && !requestedContainer.isBlank()) {
            return requestedContainer.trim();
        }

        return stageRepository.findByStageTypeAndLineAndCurrentStatus(stageType, line, StageStatus.EMPTY).stream()
                .map(ProductionStage::getContainer)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No empty container is available for the selected stage"));
    }

    public List<StageGroupResponse> getStagesByLine(String line) {

        List<ProductionStage> stages = stageRepository.findByLine(line);

        Map<String, List<ProductionStage>> grouped = stages.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStageType() + "_" + s.getStageOrder()
                ));

        return grouped.values().stream()
                .map(group -> {

                    ProductionStage first = group.get(0);

                    List<ContainerViewResponse> containers = group.stream()
                            .map(s -> ContainerViewResponse.builder()
                                    .containerName(s.getContainer())
                                    .status(s.getCurrentStatus().name())
                                    .build())
                            .toList();

                    return StageGroupResponse.builder()
                            .name(first.getName())
                            .stageType(first.getStageType().name())
                            .stageOrder(first.getStageOrder())
                            .containers(containers)
                            .build();
                })
                .sorted(Comparator.comparing(StageGroupResponse::getStageOrder))
                .toList();
    }

    public List<String> getAvailableLines() {
        return stageRepository.findAvailableLines();
    }

    public List<LineResponse> getLineOverview() {

        List<ProductionStage> stages = stageRepository.findAll();

        Map<String, List<ProductionStage>> stagesByLine =
                stages.stream().collect(Collectors.groupingBy(ProductionStage::getLine));

        List<Long> orderItemsIds = stages.stream()
                .map(ProductionStage::getOrderItemId)
                .toList();

        Map<Long, ItemResponse> orderItemResponseList =
                orderClient.getOrderItemsByIds(orderItemsIds).stream()
                        .map(i -> ItemResponse.builder()
                                .orderItemId(i.getOrderItemId())
                                .orderId(i.getOrderId())
                                .weight(i.getWeight())
                                .oliveType(i.getOliveType())
                                .build())
                        .collect(Collectors.toMap(
                                ItemResponse::getOrderItemId,
                                i -> i,
                                (a, b) -> a
                        ));

        List<LineResponse> lineResponseList = new ArrayList<>();

        for (Map.Entry<String, List<ProductionStage>> entry : stagesByLine.entrySet()) {

            List<StageResponse> stagesResponse = entry.getValue().stream()
                    .map(s -> StageResponse.builder()
                            .item(orderItemResponseList.get(s.getOrderItemId()))
                            .containerName(s.getContainer())
                            .stageStatus(s.getCurrentStatus().name())
                            .stageType(s.getStageType().name())
                            .stageId(s.getId())
                            .build())
                    .toList();

            lineResponseList.add(
                    LineResponse.builder()
                            .line(entry.getKey())
                            .stages(stagesResponse)
                            .build()
            );
        }

        return lineResponseList;
    }

    @Transactional
    public void markStorageDelivered(List<Long> storageStageIds) {

        var storages = stageRepository.findAllById(storageStageIds);

        if (storages.size() != storageStageIds.size()) {
            throw new ResourceNotFoundException("Some stages not found");
        }

        // validate first
        for (var s : storages) {
            if (s.getStageType() != StageType.STORAGE) {
                throw new IllegalArgumentException("Invalid stage type");
            }
        }

        // update items
        for (var s : storages) {
            orderClient.updateOrderItemStatus(
                    s.getOrderItemId(),
                    Map.of("status", StageStatus.COMPLETED.name())
            );

            emptyStage(s);
        }

        // fetch distinct orders
        var orderIds = storages.stream()
                .map(ProductionStage::getOrderId)
                .distinct()
                .toList();

        var orders = orderClient.getOrdersByIds(orderIds);

        Set<Long> processed = new HashSet<>();

        for (var order : orders) {

            if (!processed.add(order.getOrderId())) continue;
            if (order.getItems() == null || order.getItems().isEmpty()) continue;

            boolean allCompleted = order.getItems().stream()
                    .allMatch(i -> i.getStatus().equalsIgnoreCase("COMPLETED"));

            if (allCompleted) {
                queueClient.updateStatusByOrderId(
                        order.getOrderId(),
                        "PRODUCTION",
                        "COMPLETED"
                );
            }
        }
    }

}


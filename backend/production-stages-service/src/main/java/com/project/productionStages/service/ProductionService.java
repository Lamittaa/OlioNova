package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.*;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.exception.ServiceUnavailableException;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    // ✅ 1. Get Orders List (WITH QUEUE 🔥)
    // =========================================================
    public List<ProductionOrderListResponse> getOrders(String status, String sort) {

        List<ProductionStage> stages = stageRepository.findAll();

        Map<Long, List<ProductionStage>> grouped = stages.stream()
                .filter(s -> s.getOrderId() != null)
                .collect(Collectors.groupingBy(ProductionStage::getOrderId));

        List<ProductionOrderListResponse> result = new ArrayList<>();

        for (Long orderId : grouped.keySet()) {

            List<ProductionStage> orderStages = grouped.get(orderId);

            int totalItems = (int) orderStages.stream()
                    .map(ProductionStage::getOrderItemId)
                    .distinct()
                    .count();

            // 🔥🔥 جلب رقم الدور من Queue Service
            Integer queueNumber = null;

            try {
                queueNumber = queueClient.getQueueNumber(orderId);
            } catch (Exception e) {
                throw new ServiceUnavailableException(
                        "Queue service is not available",
                        "QUEUE_SERVICE_DOWN");
            }
            result.add(
                    ProductionOrderListResponse.builder()
                            .orderId(orderId)
                            .status("IN_PROGRESS")
                            .totalItems(totalItems)
                            .completedItems(0)
                            .queueNumber(queueNumber)
                            .build());
        }

        return result;
    }

    // =========================================================
    // ✅ 2. Get Order Details
    // =========================================================
    public OrderDetailsResponse getOrderDetails(Long orderId) {

        List<ProductionStage> stages = stageRepository.findByOrderId(orderId);

        if (stages.isEmpty()) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }

        Map<Long, List<ProductionStage>> grouped = stages.stream()
                .collect(Collectors.groupingBy(ProductionStage::getOrderItemId));

        List<OrderItemStageResponse> items = new ArrayList<>();

        for (Long itemId : grouped.keySet()) {

            List<ProductionStage> itemStages = grouped.get(itemId);

            ProductionStage current = itemStages.stream()
                    .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);

            if (current == null)
                continue;

            items.add(
                    OrderItemStageResponse.builder()
                            .orderItemId(itemId)
                            .stageType(current.getStageType().name())
                            .line(current.getLine())
                            .container(current.getContainer())
                            .oliveType("TODO")
                            .weight(0.0)
                            .build());
        }

        return OrderDetailsResponse.builder()
                .orderId(orderId)
                .status("IN_PROGRESS")
                .items(items)
                .build();
    }


    // =========================================================
    // ✅ 5. Change Stage (manual)
    // =========================================================
    public void changeStage(Long itemId, String stageTypeStr) {

        StageType newStage;

        try {
            newStage = StageType.valueOf(stageTypeStr);
        } catch (Exception e) {
            throw new BusinessRuleException(
                    "Invalid stage type",
                    "INVALID_STAGE");
        }

        ProductionStage current = getCurrentStage(itemId);

        current.setCurrentStatus(StageStatus.EMPTY);
        current.setOrderItemId(null);

        stageRepository.save(current);

        ProductionStage next = stageRepository
                .findByStageTypeAndLine(newStage, current.getLine())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stage not found for type: " + newStage));

        next.setOrderItemId(itemId);
        next.setCurrentStatus(StageStatus.IN_PROGRESS);

        stageRepository.save(next);
    }


    public MoveStageResponse changeStage(Long itemId, StageType nextStageType, String nextContainer) {

        ProductionStage current = getCurrentStage(itemId);

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
        // تنظيف الحالي
        // =========================================================
        current.setCurrentStatus(StageStatus.EMPTY);
        current.setOrderItemId(null);
        current.setOrderId(null);
        current.setContainer(null);

        stageRepository.save(current);

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
            orderClient.updateOrderStatus(
                    orderId,
                    Map.of("status", "READY_FOR_PICKUP")
            );
        }

        return MoveStageResponse.builder()
                .isLastStage(isLast)
                .message(isLast ? "Reached final stage" : "Moved to next stage")
                .build();
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

        // ✅ Already started
        if (stageRepository.existsByOrderItemId(orderItemId)) {
            throw new BusinessRuleException("Already in production", "ALREADY_STARTED");
        }

        // ✅ parse stageType
        StageType stageType;
        try {
            stageType = StageType.valueOf(request.getStageType().toUpperCase());
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid stage type", "INVALID_STAGE");
        }

        // ✅ جيب stage حسب اختيار الفني
        ProductionStage stage = stageRepository
                .findByLineAndStageTypeAndContainer(
                        request.getLine(),
                        stageType,
                        request.getContainer() // 🔥 من الفرونت
                )
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found"));

        // 🔥 أهم تحقق
        if (stage.getCurrentStatus() != StageStatus.EMPTY) {
            throw new BusinessRuleException(
                    "Container is already in use",
                    "CONTAINER_OCCUPIED"
            );
        }

        // ✅ assign
        stage.setOrderItemId(orderItemId);
        stage.setOrderId(orderId);
        stage.setCurrentStatus(StageStatus.IN_PROGRESS);

        stageRepository.save(stage);
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
                orderClient.getOrderItemssByIds(orderItemsIds).stream()
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


}
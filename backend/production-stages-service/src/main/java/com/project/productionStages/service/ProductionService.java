package com.project.productionStages.service;

import com.project.productionStages.client.OrderClient;
import com.project.productionStages.client.QueueClient;
import com.project.productionStages.dto.*;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.exception.ServiceUnavailableException;
import com.project.productionStages.mapper.ProductionMapper;
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
        // ✅ 3. Assign Line
        // =========================================================
        public void assignLine(Long itemId, String line) {

                List<ProductionStage> stages = stageRepository.findByOrderItemId(itemId);

                if (stages.isEmpty()) {
                        throw new ResourceNotFoundException("Item not found: " + itemId);
                }

                for (ProductionStage s : stages) {
                        s.setLine(line);
                }

                stageRepository.saveAll(stages);
        }

        // =========================================================
        // ✅ 4. Assign Container
        // =========================================================
        public void assignContainer(Long itemId, String container) {

                ProductionStage stage = getCurrentStage(itemId);

                long count = stageRepository.countByStageTypeAndLineAndContainerAndCurrentStatus(
                                stage.getStageType(),
                                stage.getLine(),
                                container,
                                StageStatus.IN_PROGRESS);

                if (count >= 1) {
                        throw new BusinessRuleException(
                                        "هذا الكونتينر مشغول",
                                        "CONTAINER_OCCUPIED");
                }

                stage.setContainer(container);
                stageRepository.save(stage);
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

        public void moveToNextStage(Long itemId) {

    ProductionStage current = getCurrentStage(itemId);

    StageType nextType = getNextStage(current.getStageType());

    // =========================================================
    // 🔥 إذا خلصت كل المراحل
    // =========================================================
    if (nextType == null) {

        Long orderId = current.getOrderId(); // ← احفظها قبل ما تمسحها

        current.setCurrentStatus(StageStatus.EMPTY);
        current.setOrderItemId(null);
        current.setOrderId(null); // ← نظف
        current.setContainer(null);

        stageRepository.save(current);

        try {
         orderClient.updateOrderStatus(
        orderId,
        Map.of("status", "READY_FOR_PICKUP")  // ← RequestBody
);
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                    "Order service is not available",
                    "ORDER_SERVICE_DOWN");
        }

        return;
    }

    // =========================================================
    // 🔄 الانتقال للمرحلة التالية
    // =========================================================
    Long orderId = current.getOrderId(); // ← احفظها قبل ما تمسحها

    current.setCurrentStatus(StageStatus.EMPTY);
    current.setOrderItemId(null);
    current.setOrderId(null); // ← نظف
    current.setContainer(null);

    stageRepository.save(current);

    ProductionStage next = stageRepository
            .findByStageTypeAndLine(nextType, current.getLine())
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Next stage not found"));

    next.setOrderItemId(itemId);
    next.setOrderId(orderId); // ← أضف هذا
    next.setCurrentStatus(StageStatus.IN_PROGRESS);

    stageRepository.save(next);
}

// =========================================================
// ✅ 7. Get Lines
// =========================================================
public List<LineResponse> getLines() {

    List<String> lines = List.of("A", "B");

    List<LineResponse> result = new ArrayList<>();

    for (String line : lines) {

        List<ProductionStage> stages = stageRepository.findByLine(line);

        result.add(ProductionMapper.toLineResponse(line, stages));
    }

    return result;
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

        public void startProduction(Long orderItemId, Long orderId) {

                boolean exists = stageRepository.existsByOrderItemId(orderItemId);
                if (exists) {
                        throw new BusinessRuleException("Already in production", "ALREADY_STARTED");
                }

                StageType firstStage = StageType.values()[0];

                ProductionStage stage = stageRepository
                                .findByStageType(firstStage)
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("Stage not found"));

                stage.setOrderItemId(orderItemId);
                stage.setOrderId(orderId); // ← الإضافة
                stage.setCurrentStatus(StageStatus.IN_PROGRESS);

                stageRepository.save(stage);
        }

        private StageType getNextStage(StageType current) {

                return switch (current) {
                        case CLEANING -> StageType.WASHING;
                        case WASHING -> StageType.CRUSHING;
                        case CRUSHING -> StageType.MALAXATION;
                        case MALAXATION -> StageType.EXTRACTION;
                        case EXTRACTION -> StageType.SEPARATION;
                        case SEPARATION -> StageType.STORAGE;
                        case STORAGE -> null;
                };
        }
}
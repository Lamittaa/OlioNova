package com.project.productionStages.service;

import com.project.productionStages.dto.CreateProductionBatchRequest;
import com.project.productionStages.dto.OrderItemStatusResponse;
import com.project.productionStages.dto.ProductionBatchResponse;
import com.project.productionStages.dto.UpdateBatchPredictionRequest;
import com.project.productionStages.exception.BusinessRuleException;
import com.project.productionStages.exception.ResourceNotFoundException;
import com.project.productionStages.exception.ServiceUnavailableException;
import com.project.productionStages.client.OrderClient;
import com.project.productionStages.model.ProductionBatch;
import com.project.productionStages.repository.ProductionBatchRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionBatchService {

    private final ProductionBatchRepository productionBatchRepository;
    private final OrderClient orderClient;

    public ProductionBatchResponse createBatch(CreateProductionBatchRequest request) {
        if (request.getOrderItemId() != null) {
            var existing = productionBatchRepository.findByOrderItemId(request.getOrderItemId());
            if (existing.isPresent()) {
                ProductionBatch batch = existing.get();
                batch.setOrderId(request.getOrderId());
                batch.setOliveWeightKg(request.getOliveWeightKg());
                if (request.getStatus() != null && !request.getStatus().isBlank()) {
                    batch.setStatus(request.getStatus());
                }
                return ProductionBatchResponse.from(productionBatchRepository.save(batch));
            }
        }

        String batchId = normalizeBatchId(request.getBatchId(), request.getOrderItemId());
        if (productionBatchRepository.existsById(batchId)) {
            throw new BusinessRuleException("Production batch already exists", "BATCH_ALREADY_EXISTS");
        }

        ProductionBatch batch = ProductionBatch.builder()
                .batchId(batchId)
                .orderId(request.getOrderId())
                .orderItemId(request.getOrderItemId())
                .oliveWeightKg(request.getOliveWeightKg())
                .status(request.getStatus())
                .build();

        return ProductionBatchResponse.from(productionBatchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public ProductionBatchResponse getBatch(String batchId) {
        return ProductionBatchResponse.from(findBatch(batchId));
    }

    @Transactional(readOnly = true)
    public List<ProductionBatchResponse> getBatches() {
        return productionBatchRepository.findAll().stream()
                .map(ProductionBatchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductionBatchResponse getBatchByOrderItemId(Long orderItemId) {
        return productionBatchRepository.findByOrderItemId(orderItemId)
                .map(ProductionBatchResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production batch not found for order item id: " + orderItemId));
    }

    @Transactional(readOnly = true)
    public ProductionBatchResponse getBatchByOrderId(Long orderId) {
        return productionBatchRepository.findByOrderId(orderId)
                .map(ProductionBatchResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production batch not found for order id: " + orderId));
    }

    public ProductionBatchResponse createOrGetBatchByOrderId(Long orderId) {
        var existing = productionBatchRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return ProductionBatchResponse.from(existing.get());
        }

        var order = fetchOrderForBatch(orderId);
        var oliveItems = (order.getItems() == null ? Collections.<OrderItemStatusResponse>emptyList() : order.getItems()).stream()
                .filter(this::isOliveProductionItem)
                .toList();

        if (oliveItems.isEmpty()) {
            throw new BusinessRuleException("Order does not contain an olive batch item", "ORDER_HAS_NO_OLIVE_BATCH");
        }

        if (oliveItems.size() > 1) {
            throw new BusinessRuleException("Order contains multiple olive batch items", "ORDER_HAS_MULTIPLE_OLIVE_BATCHES");
        }

        OrderItemStatusResponse item = oliveItems.get(0);
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
            throw new BusinessRuleException("Olive batch order item must include a positive quantity", "INVALID_OLIVE_BATCH_WEIGHT");
        }

        CreateProductionBatchRequest request = new CreateProductionBatchRequest();
        request.setOrderId(order.getOrderId() == null ? orderId : order.getOrderId());
        request.setOrderItemId(item.getId());
        request.setOliveWeightKg(item.getQuantity());
        request.setStatus("READY_FOR_PREDICTION");
        return createBatch(request);
    }

    public ProductionBatchResponse updatePrediction(String batchId, UpdateBatchPredictionRequest request) {
        ProductionBatch batch = findBatch(batchId);
        batch.setLatestImageId(request.getImageId());
        batch.setPredictedYieldPercent(request.getPredictedYieldPercent());
        batch.setPredictedOilKg(request.getPredictedOilKg());
        batch.setPredictionConfidence(request.getPredictionConfidence());
        batch.setModelVersion(request.getModelVersion());
        batch.setStatus("PREDICTED");
        return ProductionBatchResponse.from(productionBatchRepository.save(batch));
    }

    private ProductionBatch findBatch(String batchId) {
        return productionBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Production batch not found: " + batchId));
    }

    private boolean isOliveProductionItem(OrderItemStatusResponse item) {
        return item.getProductType() != null &&
                ("SERVICE".equalsIgnoreCase(item.getProductType()) || "OLIVE".equalsIgnoreCase(item.getProductType()));
    }

    private com.project.productionStages.dto.OrderResponse fetchOrderForBatch(Long orderId) {
        try {
            return orderClient.getOrderById(orderId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        } catch (FeignException.Forbidden ex) {
            throw new BusinessRuleException("Current user cannot read order " + orderId, "ORDER_READ_FORBIDDEN");
        } catch (FeignException.Unauthorized ex) {
            throw new BusinessRuleException("Current user is not authorized to read order " + orderId, "ORDER_READ_UNAUTHORIZED");
        } catch (FeignException ex) {
            throw new ServiceUnavailableException(
                    "Order service is currently unavailable while preparing batch " + orderId,
                    "ORDER_SERVICE_UNAVAILABLE"
            );
        }
    }

    private String normalizeBatchId(String requestedBatchId, Long orderItemId) {
        if (requestedBatchId != null && !requestedBatchId.isBlank()) {
            return requestedBatchId.trim();
        }

        String itemPart = orderItemId == null ? "NA" : orderItemId.toString();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "BATCH-%s-%s-%s".formatted(LocalDate.now(), itemPart, suffix);
    }
}

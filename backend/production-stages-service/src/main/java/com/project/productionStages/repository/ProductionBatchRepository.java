package com.project.productionStages.repository;

import com.project.productionStages.model.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, String> {
    Optional<ProductionBatch> findByOrderItemId(Long orderItemId);
    Optional<ProductionBatch> findByOrderId(Long orderId);
}

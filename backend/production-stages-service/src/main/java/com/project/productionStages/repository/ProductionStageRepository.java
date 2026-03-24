package com.project.productionStages.repository;

import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageStatus;
import com.project.productionStages.model.StageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    List<ProductionStage> findByLine(String line);

    List<ProductionStage> findByOrderId(Long orderId);

    List<ProductionStage> findByOrderItemId(Long orderItemId);

    List<ProductionStage> findByStageTypeAndLine(StageType stageType, String line);

    long countByStageTypeAndLineAndContainerAndCurrentStatus(
            StageType stageType,
            String line,
            String container,
            StageStatus status
    );

    List<ProductionStage> findByStageTypeAndLineAndCurrentStatus(
            StageType stageType,
            String line,
            StageStatus status
    );

    List<ProductionStage> findByStageType(StageType stageType);
    boolean existsByOrderItemId(Long orderItemId);
}
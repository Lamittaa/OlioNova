package com.project.productionStages.repository;

import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.model.StageStatus;
import com.project.productionStages.model.StageType;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    List<ProductionStage> findByLine(String line);

    List<ProductionStage> findByOrderId(Long orderId);

    List<ProductionStage> findByOrderItemId(Long orderItemId);
    List<ProductionStage> findByOrderItemIdIn(List<Long> itemIds);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<ProductionStage> findByLineAndStageTypeAndContainer(
        String line,
        StageType stageType,
        String container
);


List<ProductionStage> findByLineAndStageOrder(String line, Integer stageOrder);
@Query("""
    SELECT DISTINCT p.line
    FROM ProductionStage p
    WHERE p.currentStatus = 'EMPTY' AND p.stageOrder = 1
""")
List<String> findAvailableLines();
}
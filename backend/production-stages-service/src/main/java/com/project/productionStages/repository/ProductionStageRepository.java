package com.project.productionStages.repository;

import com.project.productionStages.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

  
    List<ProductionStage> findByOrderItemId(Long orderItemId);

    Optional<ProductionStage> findByOrderItemIdAndStageOrder(
            Long orderItemId,
            Integer stageOrder
    );

    Optional<ProductionStage> findByOrderItemIdAndStageType(
            Long orderItemId,
            StageType stageType
    );


    List<ProductionStage> findByLineAndIsTemplate(
            String line,
            boolean isTemplate
    );

    Optional<ProductionStage> findByLineAndCurrentStatusAndIsTemplate(
            String line,
            StageStatus status,
            boolean isTemplate
    );


    List<ProductionStage> findByLineAndStageTypeAndCurrentStatusAndIsTemplate(
            String line,
            StageType stageType,
            StageStatus status,
            boolean isTemplate
    );

    long countByLineAndCurrentStatusAndIsTemplate(
            String line,
            StageStatus status,
            boolean isTemplate
    );

    long countByLineAndStageOrderAndCurrentStatusAndIsTemplate(
            String line,
            Integer stageOrder,
            StageStatus status,
            boolean isTemplate
    );


    List<ProductionStage> findByLineAndIsTemplateOrderByStageOrderAsc(
            String line,
            boolean isTemplate
    );


    List<ProductionStage> findByCurrentStatusAndIsTemplate(
            StageStatus status,
            boolean isTemplate
    );
}
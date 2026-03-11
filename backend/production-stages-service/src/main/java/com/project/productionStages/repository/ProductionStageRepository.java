package com.project.productionStages.repository;

import com.project.productionStages.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    // =========================================================
    // ORDER STAGES — isTemplate = false
    // =========================================================

    List<ProductionStage> findByOrderItemId(Long orderItemId);

    Optional<ProductionStage> findByOrderItemIdAndStageOrder(
            Long orderItemId,
            Integer stageOrder
    );

    Optional<ProductionStage> findByOrderItemIdAndStageType(
            Long orderItemId,
            StageType stageType
    );

    // =========================================================
    // LINE QUERIES — مع isTemplate لتجنب الخلط
    // =========================================================

    // order stages فقط في خط معين
    List<ProductionStage> findByLineAndIsTemplate(
            String line,
            boolean isTemplate
    );

    // المرحلة الجارية في خط (order stages فقط)
    Optional<ProductionStage> findByLineAndCurrentStatusAndIsTemplate(
            String line,
            StageStatus status,
            boolean isTemplate
    );

    // WASHING NOT_YET في خط (للـ ETA)
    List<ProductionStage> findByLineAndStageTypeAndCurrentStatusAndIsTemplate(
            String line,
            StageType stageType,
            StageStatus status,
            boolean isTemplate
    );

    // عدد المراحل بحالة معينة في خط (order stages فقط)
    long countByLineAndCurrentStatusAndIsTemplate(
            String line,
            StageStatus status,
            boolean isTemplate
    );

    // عدد المراحل بحالة + stageOrder في خط
    long countByLineAndStageOrderAndCurrentStatusAndIsTemplate(
            String line,
            Integer stageOrder,
            StageStatus status,
            boolean isTemplate
    );

    // =========================================================
    // TEMPLATE STAGES — isTemplate = true
    // مرتبة بـ stageOrder لإنشاء Order Stages بالترتيب الصح
    // =========================================================
    List<ProductionStage> findByLineAndIsTemplateOrderByStageOrderAsc(
            String line,
            boolean isTemplate
    );

    // =========================================================
    // PIPELINE — كل المراحل الجارية (order stages فقط)
    // =========================================================
    List<ProductionStage> findByCurrentStatusAndIsTemplate(
            StageStatus status,
            boolean isTemplate
    );
}
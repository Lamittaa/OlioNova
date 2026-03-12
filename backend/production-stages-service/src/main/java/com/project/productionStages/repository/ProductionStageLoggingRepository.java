package com.project.productionStages.repository;

import com.project.productionStages.model.ProductionStageLogging;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionStageLoggingRepository
        extends JpaRepository<ProductionStageLogging, Long> {

    // =========================================================
    // ❌ كان غلط — EXTRACT(EPOCH) هو PostgreSQL SQL مش JPQL
    // ✅ استخدم nativeQuery = true لأن TIMESTAMPDIFF مش standard JPQL
    // =========================================================
@Query(value = """
SELECT AVG(EXTRACT(EPOCH FROM (l.end_time - l.start_time)) / 60.0)
FROM production_stage_logging l
WHERE l.stage_type = :stageType
AND l.end_time IS NOT NULL
""", nativeQuery = true)
Double getAverageStageMinutes(@Param("stageType") String stageType);

    // =========================================================
    // جيب كل logs لـ orderItem معين
    // =========================================================
    List<ProductionStageLogging> findByOrderItemId(Long orderItemId);

    // =========================================================
    // جيب الـ log الجاري (بدون endTime) لخط + stageOrder معين
    // بدل findAll().stream().filter(...)
    // =========================================================
    Optional<ProductionStageLogging> findByLineAndStageOrderAndEndTimeIsNull(
            String line,
            Integer stageOrder
    );

    // =========================================================
    // جيب كل logs لخط معين (للـ Dashboard)
    // بدل findAll().stream().filter(l -> l.getLine().equals(line))
    // =========================================================
    List<ProductionStageLogging> findByLine(String line);

    // =========================================================
    // جيب الـ log لطلب معين في مرحلة معينة (للـ finishStage)
    // بدل findByOrderItemId().stream().filter(stageOrder)
    // =========================================================
    Optional<ProductionStageLogging> findByOrderItemIdAndStageOrder(
            Long orderItemId,
            Integer stageOrder
    );

    // =========================================================
    // جيب كل logs جارية لخط معين (endTime = null)
    // =========================================================
    List<ProductionStageLogging> findByLineAndEndTimeIsNull(String line);
}
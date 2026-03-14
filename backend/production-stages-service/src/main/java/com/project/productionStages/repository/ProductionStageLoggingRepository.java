package com.project.productionStages.repository;

import com.project.productionStages.model.ProductionStageLogging;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionStageLoggingRepository
                extends JpaRepository<ProductionStageLogging, Long> {

        @Query(value = """
                        SELECT AVG(EXTRACT(EPOCH FROM (l.end_time - l.start_time)) / 60.0)
                        FROM production_stage_logging l
                        WHERE l.stage_type = :stageType
                        AND l.end_time IS NOT NULL
                        """, nativeQuery = true)
        Double getAverageStageMinutes(@Param("stageType") String stageType);

        List<ProductionStageLogging> findByOrderItemId(Long orderItemId);

        Optional<ProductionStageLogging> findByLineAndStageOrderAndEndTimeIsNull(
                        String line,
                        Integer stageOrder);

        List<ProductionStageLogging> findByLine(String line);

        Optional<ProductionStageLogging> findByOrderItemIdAndStageOrder(
                        Long orderItemId,
                        Integer stageOrder);

        List<ProductionStageLogging> findByLineAndEndTimeIsNull(String line);
}
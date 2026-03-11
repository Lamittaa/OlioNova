package com.project.productionStages.repository;

import com.project.productionStages.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionStageLoggingRepository extends JpaRepository<ProductionStageLogging, Long> {

    // جميع السجلات لمرحلة معينة
    List<ProductionStageLogging> findByStageType(StageType stageType);

    // جميع السجلات لطلب
    List<ProductionStageLogging> findByOrderItemId(Long orderItemId);

}
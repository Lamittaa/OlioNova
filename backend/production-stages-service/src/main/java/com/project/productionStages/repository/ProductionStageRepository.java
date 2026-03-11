package com.project.productionStages.repository;

import com.project.productionStages.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    // جميع مراحل طلب معين
    List<ProductionStage> findByOrderItemId(Long orderItemId);

    // جميع مراحل خط معين
    List<ProductionStage> findByLine(String line);

    // المرحلة الجارية في خط معين
    Optional<ProductionStage> findByLineAndCurrentStatus(String line, StageStatus status);

    // مرحلة معينة لطلب
    Optional<ProductionStage> findByOrderItemIdAndStageType(Long orderItemId, StageType stageType);

}
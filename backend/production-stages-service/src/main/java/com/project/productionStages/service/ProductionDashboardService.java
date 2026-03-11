package com.project.productionStages.service;

import com.project.productionStages.dto.ProductionDashboardResponse;
import com.project.productionStages.model.*;
import com.project.productionStages.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductionDashboardService {

    private final ProductionStageRepository stageRepository;
    private final ProductionStageLoggingRepository loggingRepository;
    private final TimeEstimationService timeService;

    public List<ProductionDashboardResponse> getDashboard() {

        String[] lines = {"A", "B"};
        List<ProductionDashboardResponse> dashboard = new ArrayList<>();

        for (String line : lines) {

            // ✅ order stages فقط (isTemplate=false) — لا templates
            Optional<ProductionStage> currentOpt =
                    stageRepository.findByLineAndCurrentStatusAndIsTemplate(
                            line,
                            StageStatus.IN_PROGRESS,
                            false
                    );

            if (currentOpt.isEmpty()) continue;

            ProductionStage current = currentOpt.get();

            Optional<ProductionStageLogging> logOpt =
                    loggingRepository.findByLineAndStageOrderAndEndTimeIsNull(
                            line,
                            current.getStageOrder()
                    );

            if (logOpt.isEmpty()) continue;

            ProductionStageLogging log = logOpt.get();

            // ✅ عدد الطلبات المنتظرة (order stages فقط)
            long queue = stageRepository.countByLineAndCurrentStatusAndIsTemplate(
                    line,
                    StageStatus.NOT_YET,
                    false
            );

            long remaining = timeService.getRemainingMinutes(
                    current.getStageType(),
                    log.getStartTime()
            );

            long avg = timeService.getAverageStageMinutes(current.getStageType());

            long eta = remaining + (avg * queue);

            int throughput = (avg > 0) ? (int) (60 / avg) : 0;

            dashboard.add(
                    ProductionDashboardResponse.builder()
                            .line(line)
                            .stage(current.getStageType())
                            .status(current.getCurrentStatus())
                            .remainingMinutes(remaining)
                            .queue((int) queue)
                            .eta(eta)
                            .avgStageTime(avg)
                            .throughputPerHour(throughput)
                            .build()
            );
        }

        return dashboard;
    }
}
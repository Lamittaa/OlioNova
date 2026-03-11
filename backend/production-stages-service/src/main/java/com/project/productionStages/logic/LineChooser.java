package com.project.productionStages.logic;

import com.project.productionStages.model.*;
import com.project.productionStages.repository.*;
import com.project.productionStages.service.TimeEstimationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LineChooser {

    private final ProductionStageRepository stageRepository;
    private final ProductionStageLoggingRepository loggingRepository;
    private final TimeEstimationService timeService;

    public String chooseBestLine() {

        String[] lines = {"A", "B"};

        String bestLine = null;
        long bestEta = Long.MAX_VALUE;

        for (String line : lines) {

            // ✅ خطأ 1 — بدل findByLine() استخدم findByLineAndIsTemplate()
            List<ProductionStage> stages =
                    stageRepository.findByLineAndIsTemplate(line, false);

            // المرحلة الحالية الجارية في الخط
            ProductionStage current = stages.stream()
                    .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                    .min((a, b) -> a.getStageOrder().compareTo(b.getStageOrder()))
                    .orElse(null);

            // الخط فارغ — أفضل خط ممكن
            if (current == null) {
                return line;
            }

            // ✅ خطأ 2 — findByLineAndStageOrderAndEndTimeIsNull يرجع Optional
            Optional<ProductionStageLogging> logOpt =
                    loggingRepository.findByLineAndStageOrderAndEndTimeIsNull(
                            line,
                            current.getStageOrder()
                    );

            if (logOpt.isEmpty()) {
                continue;
            }

            ProductionStageLogging log = logOpt.get();

            // عدد الطلبات المنتظرة في نفس المرحلة (order stages فقط)
            long queueCount =
                    stageRepository.countByLineAndStageOrderAndCurrentStatusAndIsTemplate(
                            line,
                            current.getStageOrder(),
                            StageStatus.NOT_YET,
                            false
                    );

            long eta = timeService.estimateQueueTime(
                    current.getStageType(),
                    log.getStartTime(),
                    (int) queueCount
            );

            if (eta < bestEta) {
                bestEta = eta;
                bestLine = line;
            }
        }

        return bestLine;
    }
}
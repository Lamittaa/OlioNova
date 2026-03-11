package com.project.productionStages.logic;

import com.project.productionStages.model.ProductionStage;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LineChooser {

    private final ProductionStageRepository stageRepository;

    public String chooseBestLine() {

        String[] lines = {"A", "B"};

        String bestLine = null;
        long bestRemaining = Long.MAX_VALUE;

        for (String line : lines) {

            List<ProductionStage> stages = stageRepository.findByLine(line);

            ProductionStage current = stages.stream()
                    .filter(s -> s.getStatus().name().equals("IN_PROGRESS"))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                return line;
            }

            long elapsed = Duration.between(
                    current.getStartTime(),
                    LocalDateTime.now()
            ).toMinutes();

            long avgStageTime = 10;

            long remaining = avgStageTime - elapsed;

            if (remaining < bestRemaining) {
                bestRemaining = remaining;
                bestLine = line;
            }
        }

        return bestLine;
    }
}
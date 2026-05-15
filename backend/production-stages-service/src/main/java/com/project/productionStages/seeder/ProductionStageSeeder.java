package com.project.productionStages.seeder;

import com.project.productionStages.model.*;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductionStageSeeder implements CommandLineRunner {

    private final ProductionStageRepository repository;

    @Override
    public void run(String... args) {

        if (repository.count() == 0) {

            List<ProductionStage> stages = List.of(

                    stage("Cleaning Stage", StageType.CLEANING, "A", 1, "A-CLEANING-1"),
                    stage("Washing Stage", StageType.WASHING, "A", 2, "A-WASHING-1"),
                    stage("Crushing Stage", StageType.CRUSHING, "A", 3, "A-CRUSHING-1"),
                    stage("Malaxation Stage", StageType.MALAXATION, "A", 4, "A-MALAXATION-1"),
                    stage("Extraction Stage", StageType.EXTRACTION, "A", 5, "A-EXTRACTION-1"),
                    stage("Separation Stage", StageType.SEPARATION, "A", 6, "A-SEPARATION-1"),
                    stage("Storage Stage", StageType.STORAGE, "A", 7, "A-STORAGE-1"),

                    // ===== Line B =====

                    stage("Cleaning Stage", StageType.CLEANING, "B", 1, "B-CLEANING-1"),
                    stage("Washing Stage", StageType.WASHING, "B", 2, "B-WASHING-1"),
                    stage("Crushing Stage", StageType.CRUSHING, "B", 3, "B-CRUSHING-1"),
                    stage("Malaxation Stage", StageType.MALAXATION, "B", 4, "B-MALAXATION-1"),
                    stage("Extraction Stage", StageType.EXTRACTION, "B", 5, "B-EXTRACTION-1"),
                    stage("Separation Stage", StageType.SEPARATION, "B", 6, "B-SEPARATION-1"),
                    stage("Storage Stage", StageType.STORAGE, "B", 7, "B-STORAGE-1")
            );

            repository.saveAll(stages);
        }
    }

    private ProductionStage stage(String name, StageType stageType, String line, int stageOrder, String container) {
        return ProductionStage.builder()
                .name(name)
                .stageType(stageType)
                .line(line)
                .stageOrder(stageOrder)
                .currentStatus(StageStatus.EMPTY)
                .container(container)
                .build();
    }
}

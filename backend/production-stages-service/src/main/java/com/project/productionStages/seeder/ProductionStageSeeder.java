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

                    ProductionStage.builder()
                            .name("Cleaning Stage")
                            .stageType(StageType.CLEANING)
                            .line("A")
                            .stageOrder(1)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Washing Stage")
                            .stageType(StageType.WASHING)
                            .line("A")
                            .stageOrder(2)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Crushing Stage")
                            .stageType(StageType.CRUSHING)
                            .line("A")
                            .stageOrder(3)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Malaxation Stage")
                            .stageType(StageType.MALAXATION)
                            .line("A")
                            .stageOrder(4)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Extraction Stage")
                            .stageType(StageType.EXTRACTION)
                            .line("A")
                            .stageOrder(5)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Separation Stage")
                            .stageType(StageType.SEPARATION)
                            .line("A")
                            .stageOrder(6)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Storage Stage")
                            .stageType(StageType.STORAGE)
                            .line("A")
                            .stageOrder(7)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    // ===== Line B =====

                    ProductionStage.builder()
                            .name("Cleaning Stage")
                            .stageType(StageType.CLEANING)
                            .line("B")
                            .stageOrder(1)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Washing Stage")
                            .stageType(StageType.WASHING)
                            .line("B")
                            .stageOrder(2)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Crushing Stage")
                            .stageType(StageType.CRUSHING)
                            .line("B")
                            .stageOrder(3)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Malaxation Stage")
                            .stageType(StageType.MALAXATION)
                            .line("B")
                            .stageOrder(4)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Extraction Stage")
                            .stageType(StageType.EXTRACTION)
                            .line("B")
                            .stageOrder(5)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Separation Stage")
                            .stageType(StageType.SEPARATION)
                            .line("B")
                            .stageOrder(6)
                            .currentStatus(StageStatus.EMPTY)
                            .build(),

                    ProductionStage.builder()
                            .name("Storage Stage")
                            .stageType(StageType.STORAGE)
                            .line("B")
                            .stageOrder(7)
                            .currentStatus(StageStatus.EMPTY)
                            .build()
            );

            repository.saveAll(stages);
        }
    }
}
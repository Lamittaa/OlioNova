package com.project.productionStages.seeder;

import com.project.productionStages.model.*;
import com.project.productionStages.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineTemplateSeeder implements ApplicationRunner {

    private final ProductionStageRepository stageRepository;

    // =========================================================
    // يشتغل مرة وحدة عند بدء التطبيق
    // إذا Templates موجودة → يتجاهل
    // إذا مش موجودة → يُنشئها
    // =========================================================
    @Override
    public void run(ApplicationArguments args) {

        boolean alreadySeeded = !stageRepository
                .findByLineAndIsTemplateOrderByStageOrderAsc("A", true)
                .isEmpty();

        if (alreadySeeded) {
            log.info("Pipeline templates already exist — skipping seeder");
            return;
        }

        log.info("Seeding pipeline templates...");

        List<TemplateDefinition> definitions = List.of(

                // ===== Line A =====
                new TemplateDefinition("A", StageType.WASHING,    1),
                new TemplateDefinition("A", StageType.CRUSHING,   2),
                new TemplateDefinition("A", StageType.MALAXATION, 3),
                new TemplateDefinition("A", StageType.PRESSING,   4),
                new TemplateDefinition("A", StageType.STORAGE,    5),

                // ===== Line B =====
                new TemplateDefinition("B", StageType.WASHING,    1),
                new TemplateDefinition("B", StageType.CRUSHING,   2),
                new TemplateDefinition("B", StageType.MALAXATION, 3),
                new TemplateDefinition("B", StageType.PRESSING,   4),
                new TemplateDefinition("B", StageType.STORAGE,    5)
        );

        for (TemplateDefinition def : definitions) {

            ProductionStage template = ProductionStage.builder()
                    .name(def.stageType().name())
                    .stageType(def.stageType())
                    .line(def.line())
                    .stageOrder(def.stageOrder())
                    .currentStatus(StageStatus.NOT_YET)
                    .isTemplate(true)
                    // orderId + orderItemId = null عشان template
                    .build();

            stageRepository.save(template);
        }

        log.info("Pipeline templates seeded successfully — {} templates created", definitions.size());
    }

    // =========================================================
    // Record بسيط لتعريف كل template
    // =========================================================
    private record TemplateDefinition(
            String line,
            StageType stageType,
            int stageOrder
    ) {}
}
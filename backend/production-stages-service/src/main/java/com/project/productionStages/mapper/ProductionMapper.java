package com.project.productionStages.mapper;

import com.project.productionStages.dto.*;
import com.project.productionStages.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class ProductionMapper {

    public static LineResponse toLineResponse(String line, List<ProductionStage> stages) {

        Map<StageType, List<ProductionStage>> groupedByStage =
                stages.stream().collect(Collectors.groupingBy(ProductionStage::getStageType));

        List<StageResponse> stageResponses = new ArrayList<>();

        // ✅ ترتيب ثابت للمراحل
        List<StageType> orderedTypes = List.of(
                StageType.CLEANING,
                StageType.WASHING,
                StageType.CRUSHING,
                StageType.MALAXATION,
                StageType.EXTRACTION,
                StageType.SEPARATION,
                StageType.STORAGE
        );

        for (StageType stageType : orderedTypes) {

            if (!groupedByStage.containsKey(stageType)) continue;

            List<ProductionStage> stageList = groupedByStage.get(stageType);

            // MULTI stage (MALAXATION / STORAGE)
            if (isMultiStage(stageType)) {

                Map<String, List<ProductionStage>> groupedByContainer =
                        stageList.stream().collect(Collectors.groupingBy(
                                s -> Optional.ofNullable(s.getContainer()).orElse("UNKNOWN")
                        ));

                List<ContainerResponse> containers = new ArrayList<>();

                List<String> containerNames = getContainerNames(stageType);

                for (String c : containerNames) {

                    List<ProductionStage> items = groupedByContainer.getOrDefault(c, List.of());

                    containers.add(
                            ContainerResponse.builder()
                                    .name(getContainerLabel(stageType, c))
                                    .items(items.stream()
                                            .map(ProductionMapper::toItemResponse)
                                            .toList())
                                    .build()
                    );
                }

                stageResponses.add(
                        StageResponse.builder()
                                .stageType(stageType.name())
                                .stageLabel(getStageLabel(stageType))
                                .containers(containers)
                                .build()
                );

            } else {

                ProductionStage stage = stageList.stream()
                        .filter(s -> s.getCurrentStatus() == StageStatus.IN_PROGRESS)
                        .findFirst()
                        .orElse(null);

                stageResponses.add(
                        StageResponse.builder()
                                .stageType(stageType.name())
                                .stageLabel(getStageLabel(stageType))
                                .item(stage != null ? toItemResponse(stage) : null)
                                .build()
                );
            }
        }

        return LineResponse.builder()
                .line(line)
                .stages(stageResponses)
                .build();
    }

    // 🔹 Item mapping
    public static ItemResponse toItemResponse(ProductionStage stage) {

        return ItemResponse.builder()
                .orderItemId(stage.getOrderItemId())
                .orderId(stage.getOrderId())
                .oliveType("TODO") // تجيبها من order-service
                .weight(0.0)       // تجيبها من order-service
                .build();
    }

    // 🔹 helper: multi stages
    private static boolean isMultiStage(StageType stageType) {
        return stageType == StageType.MALAXATION || stageType == StageType.STORAGE;
    }

    // 🔹 helper: container names
    private static List<String> getContainerNames(StageType stageType) {

        if (stageType == StageType.MALAXATION) {
            return List.of("1", "2", "3", "4");
        }

        if (stageType == StageType.STORAGE) {
            return List.of("A", "B", "C", "D");
        }

        return List.of();
    }

    // 🔹 helper: label
    private static String getContainerLabel(StageType stageType, String c) {

        if (stageType == StageType.MALAXATION) {
            return "عجين " + c;
        }

        if (stageType == StageType.STORAGE) {
            return "خزان " + c;
        }

        return c;
    }

    // 🔹 helper: stage label
    private static String getStageLabel(StageType stageType) {

        return switch (stageType) {
            case CLEANING   -> "تنظيف";
            case WASHING    -> "غسل";
            case CRUSHING   -> "طحن";
            case MALAXATION -> "عجن";
            case EXTRACTION -> "عصر";
            case SEPARATION -> "فصل";
            case STORAGE    -> "تخزين";
        };
    }
}
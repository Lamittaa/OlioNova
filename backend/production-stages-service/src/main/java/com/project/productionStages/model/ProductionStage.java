package com.project.productionStages.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "production_stage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private StageType stageType;

    private Long orderId;

    private Long orderItemId;

    private String line;

    private Integer stageOrder;

    @Enumerated(EnumType.STRING)
    private StageStatus currentStatus;
    @Builder.Default
    private boolean isTemplate = false;
}
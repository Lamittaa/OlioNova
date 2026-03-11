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

    // =========================================================
    // orderId + orderItemId = null فقط إذا isTemplate = true
    // =========================================================
    private Long orderId;

    private Long orderItemId;

    private String line;

    private Integer stageOrder;

    @Enumerated(EnumType.STRING)
    private StageStatus currentStatus;

    // =========================================================
    // ✅ إضافة جديدة — يفرق بين Template Stage و Order Stage
    //
    // isTemplate = true  → مرحلة ثابتة للخط (orderId = null)
    // isTemplate = false → مرحلة لطلب حقيقي  (orderId = X)
    // =========================================================
    @Builder.Default
    private boolean isTemplate = false;
}
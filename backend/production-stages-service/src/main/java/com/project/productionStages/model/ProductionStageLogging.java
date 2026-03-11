package com.project.productionStages.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_stage_logging")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionStageLogging {

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

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long employeeId;
}
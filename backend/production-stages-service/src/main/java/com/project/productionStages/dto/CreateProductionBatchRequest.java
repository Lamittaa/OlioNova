package com.project.productionStages.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductionBatchRequest {

    private String batchId;

    private Long orderId;

    @NotNull
    private Long orderItemId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal oliveWeightKg;

    private String status;
}

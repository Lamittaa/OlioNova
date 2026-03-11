package com.project.productionStages.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartProductionRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "orderItemId is required")
    private Long orderItemId;
}
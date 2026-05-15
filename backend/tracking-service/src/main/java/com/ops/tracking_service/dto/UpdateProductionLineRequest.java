package com.ops.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateProductionLineRequest {
    @NotBlank
    private String productionLine;

    public String getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(String productionLine) {
        this.productionLine = productionLine;
    }
}

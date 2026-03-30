package com.project.productionStages.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class 
OrderItemStatusResponse {

    private Long id;
    private String status;
    private String productType;
}
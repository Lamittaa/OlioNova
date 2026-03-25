package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderItemResponse {

    private Long orderItemId;

    private Long orderId;

    private String oliveType;

    private Double weight;

}

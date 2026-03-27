package com.project.productionStages.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long orderItemId;

    private Long orderId;

    private String oliveType;

    private Double weight;

}

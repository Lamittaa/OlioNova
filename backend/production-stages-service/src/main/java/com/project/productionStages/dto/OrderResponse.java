package com.project.productionStages.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    @JsonAlias("id")
    private Long orderId;

    private String status;

    private List<OrderItemStatusResponse> items;
}

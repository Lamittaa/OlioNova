package com.project.productionStages.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OrdersDashboardDto {


    private Long orderId;

    private String status;

//    private Integer totalItems;
//    private Integer completedItems;

    private List<OrderItemStatusResponse> items;
}

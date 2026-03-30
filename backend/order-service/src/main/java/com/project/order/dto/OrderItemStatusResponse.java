package com.project.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemStatusResponse {

    private Long id;
    private String status;
    private String productType;
}
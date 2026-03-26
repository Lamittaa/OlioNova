package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemBulkResponse {

      private Long orderItemId;

    private Long orderId;

    private String oliveType;

    private Double weight;
}

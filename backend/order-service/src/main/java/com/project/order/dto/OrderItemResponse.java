package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class OrderItemResponse {

  private Long orderItemId;

    private Long orderId;

    private String oliveType;

    private Double weight;
}
package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FulfillResponse {

    private Long    orderId;
    private Long    itemId;
    private String  productName;
    private String  productType;
    private Integer quantity;
    private String  status;
}
package com.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderSummaryResponse {

    private Long   id;
    private Long   customerId;
    private String status;
    private double totalPrice;
    private List<OrderItemSummary> items;

    @JsonProperty("isMember")
    private boolean isMember;
}
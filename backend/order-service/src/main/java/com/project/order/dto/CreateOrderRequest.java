package com.project.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Order items are required")
    @Size(min = 1, message = "Order must contain at least one item")
    private List<@Valid CreateOrderItemRequest> items;
}

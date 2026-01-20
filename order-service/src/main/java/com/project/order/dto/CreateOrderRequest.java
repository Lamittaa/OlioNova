package com.project.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @NotNull(message = "Items cannot be null")
    @Size(min = 1, message = "Order must contain at least 1 item")
    private List<@Valid CreateOrderItemRequest> items;
}

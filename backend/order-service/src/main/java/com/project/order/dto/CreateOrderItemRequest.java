package com.project.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateOrderItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private BigDecimal quantity;

    private String oliveType;
    private Integer bagsCount;
    private String note;
}

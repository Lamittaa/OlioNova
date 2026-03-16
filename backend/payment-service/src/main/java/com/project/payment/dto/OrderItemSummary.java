package com.project.payment.dto;

import lombok.Getter;
import lombok.Setter;

// OrderItemSummary — quantity هو الوزن للـ SERVICE
@Getter @Setter
public class OrderItemSummary {
    private Long    id;
    private Long    productId;
    private String  productName;
    private String  productType;  // SERVICE / PURCHASE
    private String  unit;         // KG / PIECE
    private int     quantity;     // الوزن للـ SERVICE، الكمية للـ PURCHASE
    private double  price;
    private String  oliveType;
    private Integer bagsCount;
    private String  status;
    private double  totalPrice;
}
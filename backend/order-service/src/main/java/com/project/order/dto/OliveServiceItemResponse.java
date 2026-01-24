package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OliveServiceItemResponse extends OrderItemResponse {

    private String oliveType;
    private Integer bagsCount;
    private String note;

    
}

package com.project.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CustomerByNationalIdResponse {
    private Long id;
    private String nationalId;
    private String fullName;
}

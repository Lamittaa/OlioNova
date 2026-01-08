package com.project.customer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerResponse {
    private Long id;
    private String nationalId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Long cityId;
    private Boolean isMember;
}

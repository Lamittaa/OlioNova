package com.project.customer.dto;

import lombok.*;

@Getter
@Setter
public class CreateCustomerResponse {

    private Long id;
    private String nationalId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Long cityId;

}

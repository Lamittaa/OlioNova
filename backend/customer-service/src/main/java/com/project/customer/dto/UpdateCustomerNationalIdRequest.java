package com.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerNationalIdRequest {

    @NotBlank(message = "National ID must not be empty")
    @Pattern(regexp = "^[0-9]{9}$", message = "National ID must be exactly 9 digits")
    private String nationalId;
}

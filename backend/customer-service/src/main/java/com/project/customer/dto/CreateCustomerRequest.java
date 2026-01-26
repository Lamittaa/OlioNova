package com.project.customer.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class CreateCustomerRequest {


    @NotBlank(message = "National ID cannot be empty")
    @Pattern(
        regexp = "^[0-9]{9}$",
        message = "National ID must contain exactly 9 digits"
    )
    private String nationalId;

      @NotBlank(message = "First name cannot be empty")
    @Size(max = 100, message = "First name must be at most 100 characters")
    @Pattern(
        regexp = "^[\\p{L}]+(?:[\\s]+[\\p{L}]+)*$",
        message = "First name must contain letters only and may include spaces (e.g., عبد الفتاح)"
    )
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[A-Za-zء-ي\\s]+$",
        message = "Last name must contain letters only"
    )
    private String lastName;

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(
        regexp = "^(05|\\+9705)[0-9]{8}$",
        message = "Phone number must be a valid Palestinian mobile number"
    )
    private String phoneNumber;

    @NotNull(message = "City ID cannot be null")
    private Long cityId;

    private Boolean isMember;

}

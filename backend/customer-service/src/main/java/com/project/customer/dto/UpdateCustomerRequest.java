package com.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {
  
     @NotBlank(message = "First name cannot be empty or whitespace")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[A-Za-zء-ي\\s]+$",
        message = "First name must contain letters only"
    )
    private String firstName;

    @NotBlank(message = "Last name cannot be empty or whitespace")
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
}

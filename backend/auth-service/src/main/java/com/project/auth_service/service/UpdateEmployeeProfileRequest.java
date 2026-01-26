package com.project.auth_service.service;

import com.project.auth_service.model.MaritalStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeProfileRequest {

    private String firstName;
    private String lastName;

    @Pattern(
        regexp = "^05[0-9]{8}$",
        message = "Invalid phone number"
    )
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private MaritalStatus maritalStatus;
}

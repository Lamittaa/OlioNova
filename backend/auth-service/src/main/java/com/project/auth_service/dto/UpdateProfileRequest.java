package com.project.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String firstName;

    private String lastName;

    private String phoneNumber;

    @Email
    private String email;

    private String city;

    @Pattern(regexp = "SINGLE|MARRIED|DIVORCED|WIDOWED")
    private String maritalStatus;
}

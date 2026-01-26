package com.project.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {

    private String firstName;
    private String lastName;

    @Pattern(
        regexp = "^05[0-9]{8}$",
        message = "Invalid phone number"
    )
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    // فقط للأدمن
    private String roleName;
    private Boolean enabled;
}

package com.project.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {

    @Size(max = 20)
    private String nationalId;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 20)
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String gender;
    private String maritalStatus;
    private String roleName;
    private Boolean enabled;
}

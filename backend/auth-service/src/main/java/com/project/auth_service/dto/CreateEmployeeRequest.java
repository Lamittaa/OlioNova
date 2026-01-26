package com.project.auth_service.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {

    @NotBlank
    @Size(max = 20)
    private String nationalId;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    private String phoneNumber;

    @Email
    @NotBlank
    @Size(max = 100)
    private String email;

    @NotNull
    private String gender;          // MALE / FEMALE (enum check في service)

    @NotNull
    private String maritalStatus;   // SINGLE / MARRIED

    @NotBlank
    private String roleName;        // ADMIN / ACCOUNTANT / RECEPTIONIST / TECHNICIAN
}

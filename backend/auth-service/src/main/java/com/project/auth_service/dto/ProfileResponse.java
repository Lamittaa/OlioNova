package com.project.auth_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {

    private Long id;
    private String nationalId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String city;
    private String gender;
    private String maritalStatus;
    private String username;
    private String role;
}

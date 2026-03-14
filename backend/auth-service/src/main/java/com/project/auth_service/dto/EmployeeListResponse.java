package com.project.auth_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeListResponse {

    private Long id;
    private String fullName;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private String username;
    private String role;
    private boolean enabled;
}

package com.project.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType;     // "Bearer"
    private long   expiresIn;
    private long refreshExpiresIn;     // seconds for access token
    private String username;      // اسم المستخدم الحالي
    private String role;          // دوره في النظام
}

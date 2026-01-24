package com.project.auth_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RefreshResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType;   // "Bearer"
    private long   expiresIn;   // seconds for access token
private long  refreshExpiresIn;   // seconds for refresh token
    private String username;
    private String role;
}

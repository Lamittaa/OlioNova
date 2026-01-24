package com.project.auth_service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {
    private String username;
    private String email;
    private String role;
    private List<String> authorities;
}

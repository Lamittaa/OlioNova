package com.project.auth_service.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleAuthoritiesResponseDto {
    private Long roleId;
    private String roleName;
    private List<String> authorities;
    private String message;
}

package com.project.auth_service.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {
    private Long id;
    private String name;
    private List<String> authorities;
}

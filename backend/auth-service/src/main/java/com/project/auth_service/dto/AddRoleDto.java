package com.project.auth_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddRoleDto {
    
    @NotBlank(message = "Role name cannot be blank")
    @Pattern(
        regexp = "^[A-Za-z]+$",
        message = "Role name must contain letters only and no numbers"
    )
    private String name;
}

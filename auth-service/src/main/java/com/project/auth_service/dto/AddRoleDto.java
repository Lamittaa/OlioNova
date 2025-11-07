package com.project.auth_service.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddRoleDto {
    @NotBlank(message = "Role name cannot be blank")
    private String name;
}

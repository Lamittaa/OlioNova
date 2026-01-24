package com.project.auth_service.dto;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class LoginRequestDto {
    @NotBlank(message = "username must not be blank")
    private String username;

    @NotBlank(message = "password must not be blank")
    private String password;
}

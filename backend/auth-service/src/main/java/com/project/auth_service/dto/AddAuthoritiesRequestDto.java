package com.project.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddAuthoritiesRequestDto {
    @NotEmpty(message = "Authorities list cannot be empty")

    private List<@NotBlank(message = "Authority must not be blank") @Pattern(regexp = "^[A-Za-z]+$", message = "Role name must contain letters only and no numbers") String> authorities;
}

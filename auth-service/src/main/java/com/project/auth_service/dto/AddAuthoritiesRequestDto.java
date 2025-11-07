package com.project.auth_service.dto;

import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddAuthoritiesRequestDto {
    @NotEmpty(message = "Authorities list cannot be empty")
    private List<String> authorities;
}


package com.project.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RevokeRequestDto {

    @NotBlank(message = "access Token must not be blank")
    private String accessToken;

}

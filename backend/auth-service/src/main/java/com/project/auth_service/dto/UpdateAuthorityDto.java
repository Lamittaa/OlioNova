package com.project.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAuthorityDto {

    @NotBlank(message = "Authority name must not be blank")
    private String name;
}

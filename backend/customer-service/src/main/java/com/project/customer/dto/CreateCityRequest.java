package com.project.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCityRequest {

 @NotBlank(message = "City name cannot be empty or whitespace")
    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    @Pattern(
        regexp = "^[A-Za-zء-ي\\s]+$",
        message = "City name must contain letters only (no digits or symbols)"
    )
    private String cityName;
}

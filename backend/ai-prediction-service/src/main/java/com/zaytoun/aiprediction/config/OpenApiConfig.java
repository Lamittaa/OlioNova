package com.zaytoun.aiprediction.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "AI Prediction Service API",
                version = "v1",
                description = "Predicts olive oil yield percentage from RGB images of olive fruit samples.",
                contact = @Contact(name = "Zaytoun Team")
        ),
        security = {
                @SecurityRequirement(name = "apiKeyAuth")
        }
)
@SecurityScheme(
        name = "apiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER,
        paramName = "X-API-Key"
)
public class OpenApiConfig {
}

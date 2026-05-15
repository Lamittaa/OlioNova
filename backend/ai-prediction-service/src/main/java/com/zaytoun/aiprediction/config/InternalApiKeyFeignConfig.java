package com.zaytoun.aiprediction.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalApiKeyFeignConfig {

    @Bean
    RequestInterceptor internalApiKeyRequestInterceptor(
            @Value("${security.api-key.internal}") String internalApiKey) {
        return template -> template.header("X-API-Key", internalApiKey);
    }
}

package com.ops.tracking_service.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardConfig {

    @Value("${security.api-key.internal:}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                if (!internalApiKey.isBlank()) {
                    template.header("X-API-Key", internalApiKey);
                }
                return;
            }

            HttpServletRequest request = servletAttributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null && !authorization.isBlank()) {
                template.header("Authorization", authorization);
            } else if (!internalApiKey.isBlank()) {
                template.header("X-API-Key", internalApiKey);
            }
        };
    }
}

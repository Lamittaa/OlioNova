package com.project.productionStages.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {

        return new RequestInterceptor() {

            @Override
            public void apply(RequestTemplate template) {

                RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

                if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                    return;
                }

                HttpServletRequest request = servletAttributes.getRequest();

                String authorization = request.getHeader("Authorization");

                if (authorization != null && !authorization.isEmpty()) {
                    template.header("Authorization", authorization);
                }
            }
        };
    }
}
package com.project.payment.security;

import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private final SecurityErrorHandler securityErrorHandler;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Swagger public
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                    
                ).permitAll()

                // health check
                .requestMatchers("/actuator/health").permitAll()

                // باقي الطلبات لازم JWT
                .anyRequest().authenticated()
            )

            // ⭐ مهم: يخلي 401/403 يطلعوا JSON عبر SecurityErrorHandler
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                // ⭐ كمان هون للضمان
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)

                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // HS256
        var key = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // لازم يطابق اسم الـ claim داخل الـ JWT
        authoritiesConverter.setAuthoritiesClaimName("authorities");

        // إذا عندك authorities مثل ROLE_ADMIN و PRODUCT_CREATE ... خليها ""
        // (يعني ما يضيف ROLE_ زيادة)
        authoritiesConverter.setAuthorityPrefix("");

        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return jwtConverter;
    }
}

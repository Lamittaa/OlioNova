package com.project.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class GatewayJwtFilter implements GlobalFilter, Ordered {

    private final Key key;

    public GatewayJwtFilter(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // ✅ Public endpoints
        if (path.startsWith("/api/auth")
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")) {
            return chain.filter(exchange);
        }

        // ✅ CORS preflight
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        System.out.println("[GATEWAY] path=" + path);
        System.out.println("[GATEWAY] Authorization=" + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            // ✅ الحل: ننسخ الهيدرز ونضيف عليهم بدون ما نعدل ReadOnlyHttpHeaders
            ServerHttpRequest originalRequest = exchange.getRequest();

            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.putAll(originalRequest.getHeaders());
            newHeaders.set("X-Username", username);

            ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(originalRequest) {
                @Override
                public HttpHeaders getHeaders() {
                    return newHeaders;
                }
            };

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(decoratedRequest)
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            System.out.println("[GATEWAY JWT ERROR] " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

// src/main/java/com/project/auth_service/security/JwtAuthFilter.java
package com.project.auth_service.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.auth_service.service.CustomUserDetailsService;
import com.project.auth_service.service.JwtService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final CustomUserDetailsService uds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // استثناء المسارات العامة + preflight
        String path = request.getServletPath();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        return path.startsWith("/api/auth/login")
            || path.startsWith("/api/auth/register")
            || path.startsWith("/api/auth/refresh")    // refresh لا يحتاج Access
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-ui")
            || path.equals("/swagger-ui.html")
            || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                String username = jwt.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = uds.loadUserByUsername(username);

                    // نتحقق أنه Access Token (صالح وغير منتهي)
                    if (jwt.isAccessTokenValid(token, user)) {
                        // نقرأ الأدوار من الـ access؛ ولو مفقودة ن fallback لأدوار الـ user
                        List<String> roles = jwt.extractRoles(token);
                        if (roles == null || roles.isEmpty()) {
                            roles = user.getAuthorities().stream()
                                    .map(a -> a.getAuthority()).toList();
                        }

                        var authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        var authToken = new UsernamePasswordAuthenticationToken(
                                user, null, authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (ExpiredJwtException ex) {
                // access منتهي: نمرّر الطلب بدون مصادقة (العميل يجدد عبر /auth/refresh)
                // ممكن تضيف header توضيحي:
                response.setHeader("X-Token-Expired", "true");
            } catch (JwtException | IllegalArgumentException ex) {
                // توكن غير صالح — لا نرمي 500
                // ممكن logging فقط
            }
        }

        chain.doFilter(request, response);
    }
}

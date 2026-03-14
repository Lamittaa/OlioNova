package com.project.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.order.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public SecurityErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
            org.springframework.security.core.AuthenticationException ex)
            throws IOException, ServletException {

        String code = "UNAUTHORIZED";
        String msg = "Unauthorized";

        Throwable cause = ex.getCause();
        if (cause instanceof JwtException) {
            code = "INVALID_TOKEN";
            msg = "Invalid or expired token";
        }
        if (ex instanceof OAuth2AuthenticationException oae) {
            code = "INVALID_TOKEN";
            msg = oae.getError().getDescription() != null ? oae.getError().getDescription() : msg;
        }

        write(res, HttpStatus.UNAUTHORIZED, req.getRequestURI(), code, msg);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
            AccessDeniedException ex) throws IOException, ServletException {

        write(res, HttpStatus.FORBIDDEN, req.getRequestURI(), "FORBIDDEN", "Forbidden");
    }

    private void write(HttpServletResponse res, HttpStatus status, String path, String code, String msg)
            throws IOException {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(msg)
                .path(path)
                .code(code)
                .errors(null)
                .build();

        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), body);
    }
}

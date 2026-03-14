package com.project.auth_service.controller;

import com.project.auth_service.service.AuthService;
import com.project.auth_service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<RefreshResponseDto> refresh(@Valid @RequestBody RefreshRequestDto dto) {
        return ResponseEntity.ok(authService.refresh(dto));
    }

    @PostMapping("/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> logout() {
        RevokeRequestDto revokeRequestDto = new RevokeRequestDto();
        String userName = ((UserDetails) (SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
                .getUsername();
        authService.logout(userName);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout successful — refresh token revoked");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/set-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@Valid @RequestBody SetPasswordRequest req) {
        authService.setPassword(req);
    }

}

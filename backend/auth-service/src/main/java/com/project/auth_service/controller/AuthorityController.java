package com.project.auth_service.controller;

import com.project.auth_service.dto.*;
import com.project.auth_service.service.AuthorityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/authorities")
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityService authorityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorityDto> addAuthority(
            @Valid @RequestBody AddAuthorityDto dto) {

        log.info("[ADMIN] Create authority: {}", dto.getName());
        AuthorityDto result = authorityService.addAuthority(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthorityDto>> getAllAuthorities() {

        log.info("[ADMIN] Fetch all authorities");
        return ResponseEntity.ok(authorityService.getAllAuthorities());
    }

    @GetMapping("/{authorityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorityDto> getAuthorityById(
            @PathVariable Long authorityId) {

        log.info("[ADMIN] Fetch authority {}", authorityId);
        return ResponseEntity.ok(authorityService.getAuthorityById(authorityId));
    }

    @PutMapping("/{authorityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthorityDto> updateAuthority(
            @PathVariable Long authorityId,
            @Valid @RequestBody UpdateAuthorityDto dto) {

        log.info("[ADMIN] Update authority {} -> {}", authorityId, dto.getName());
        return ResponseEntity.ok(
                authorityService.updateAuthority(authorityId, dto));
    }

    @DeleteMapping("/{authorityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAuthority(
            @PathVariable Long authorityId) {

        log.info("[ADMIN] Delete authority {}", authorityId);
        authorityService.deleteAuthority(authorityId);
        return ResponseEntity.ok("Authority deleted successfully");
    }
}

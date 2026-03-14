package com.project.auth_service.controller;

import com.project.auth_service.dto.*;
import com.project.auth_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddRoleResponseDto> addRole(@Valid @RequestBody AddRoleDto roleDto) {
        log.info("[ADMIN] Create new role: {}", roleDto.getName());
        AddRoleResponseDto result = roleService.addRole(roleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        log.info("[ADMIN] Fetch all roles");
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleAuthoritiesResponseDto> getAuthoritiesByRole(@PathVariable Long roleId) {
        log.info("[ADMIN] Fetch authorities for role {}", roleId);
        RoleAuthoritiesResponseDto response = roleService.getAuthoritiesDto(roleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roleId}/authorities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleAuthoritiesResponseDto> addAuthoritiesToRole(
            @PathVariable Long roleId,
            @Valid @RequestBody AddAuthoritiesRequestDto request) {

        log.info("[ADMIN] Add authorities {} to role {}", request.getAuthorities(), roleId);
        RoleAuthoritiesResponseDto response = roleService.addAuthoritiesToRole(roleId, request.getAuthorities());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{roleId}/authorities/{authorityName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleAuthoritiesResponseDto> removeAuthorityFromRole(
            @PathVariable Long roleId,
            @PathVariable String authorityName) {

        log.info("[ADMIN] Remove authority '{}' from role ID {}", authorityName, roleId);
        RoleAuthoritiesResponseDto response = roleService.removeAuthorityFromRole(roleId, authorityName);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<String> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok("Role deleted successfully");
    }

}

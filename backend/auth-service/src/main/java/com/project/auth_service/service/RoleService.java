package com.project.auth_service.service;

import com.project.auth_service.dto.*;
import com.project.auth_service.exception.AuthorityNotAssignedException;
import com.project.auth_service.exception.EntityAlreadyExistsException;
import com.project.auth_service.exception.EntityInUseException;
import com.project.auth_service.exception.RoleNotFoundException;
import com.project.auth_service.model.Authority;
import com.project.auth_service.model.Role;
import com.project.auth_service.repository.AuthorityRepository;
import com.project.auth_service.repository.RoleRepository;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.mapper.RoleMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
     private final UserRepository userRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public AddRoleResponseDto addRole(AddRoleDto roleDto) {
        roleDto.setName(roleDto.getName().toUpperCase());
        if (roleRepository.findByName(roleDto.getName()).isPresent()) {
            throw new EntityAlreadyExistsException("Role already exists: " + roleDto.getName());
        }
        Role saved = roleRepository.save(roleMapper.toEntity(roleDto));
        log.info("[ROLE] Role '{}' created successfully", saved.getName());
        return roleMapper.toAddRoleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toRoleDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleAuthoritiesResponseDto getAuthoritiesDto(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID=" + roleId));

        return RoleAuthoritiesResponseDto.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .authorities(role.getAuthorities().stream().map(Authority::getName).toList())
                .message("Authorities retrieved successfully")
                .build();
    }

    @Transactional
    public RoleAuthoritiesResponseDto addAuthoritiesToRole(Long roleId, List<String> authorityNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID=" + roleId));

        List<Authority> authorities = authorityRepository.findByNameInIgnoreCase(authorityNames);
        if (authorities.isEmpty()) {
            throw new EntityNotFoundException("No authorities found with names: " + authorityNames);
        }

        for (Authority a : authorities) {
            if (role.getAuthorities().contains(a)) {
                throw new EntityAlreadyExistsException("Authority already exists in role: " + a.getName());
            }
            role.getAuthorities().add(a);
        }

        roleRepository.save(role);

        log.info("[ROLE] Authorities {} added to role '{}'", authorityNames, role.getName());

        return RoleAuthoritiesResponseDto.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .authorities(role.getAuthorities().stream().map(Authority::getName).toList())
                .message("Authorities added successfully to role")
                .build();
    }

@Transactional
public RoleAuthoritiesResponseDto removeAuthorityFromRole(Long roleId, String authorityName) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Role not found with ID=" + roleId));

    Authority authority = authorityRepository.findByNameIgnoreCase(authorityName)
            .orElseThrow(() -> new EntityNotFoundException("Authority not found: " + authorityName));

    if (!role.getAuthorities().contains(authority)) {
        throw new AuthorityNotAssignedException(
                String.format("Role '%s' (ID=%d) does not contain authority: %s",
                        role.getName(), role.getId(), authorityName)
        );
    }

    role.getAuthorities().remove(authority);
    roleRepository.save(role);

    log.info("[ROLE] Authority '{}' removed from role '{}'", authorityName, role.getName());

    return RoleAuthoritiesResponseDto.builder()
            .roleId(role.getId())
            .roleName(role.getName())
            .authorities(role.getAuthorities().stream().map(Authority::getName).toList())
            .message("Authority '" + authorityName + "' removed successfully from role")
            .build();
}
@Transactional
public void deleteRole(Long roleId) {
    Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RoleNotFoundException("Role with ID " + roleId + " not found"));

    boolean roleInUse = userRepository.existsByRole_Id(roleId);
    if (roleInUse) {
        throw new EntityInUseException("Cannot delete role because it is assigned to existing users.");
    }

    roleRepository.delete(role);
}


}



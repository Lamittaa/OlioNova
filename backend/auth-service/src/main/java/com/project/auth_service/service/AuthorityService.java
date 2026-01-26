package com.project.auth_service.service;

import com.project.auth_service.dto.AddAuthorityDto;
import com.project.auth_service.dto.AuthorityDto;
import com.project.auth_service.dto.UpdateAuthorityDto;
import com.project.auth_service.exception.EntityInUseException;
import com.project.auth_service.mapper.AuthorityMapper;
import com.project.auth_service.model.Authority;
import com.project.auth_service.repository.AuthorityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final AuthorityMapper authorityMapper;

    // ✅ Create
    public AuthorityDto addAuthority(AddAuthorityDto dto) {

        if (authorityRepository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException(
                    "Authority already exists: " + dto.getName()
            );
        }

        Authority authority = authorityMapper.toEntity(dto);
        Authority saved = authorityRepository.save(authority);

        return authorityMapper.toDto(saved);
    }

    // ✅ Read all
    @Transactional(readOnly = true)
    public List<AuthorityDto> getAllAuthorities() {

        return authorityRepository.findAll()
                .stream()
                .map(authorityMapper::toDto)
                .toList();
    }

    // ✅ Read by ID

    @Transactional(readOnly = true)
    public AuthorityDto getAuthorityById(Long id) {

        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Authority not found with id: " + id)
                );

        return authorityMapper.toDto(authority);
    }

    // ✅ Update

    public AuthorityDto updateAuthority(Long id, UpdateAuthorityDto dto) {

        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Authority not found with id: " + id)
                );

        if (!authority.getName().equals(dto.getName())
            && authorityRepository.existsByName(dto.getName())) {
            throw new DataIntegrityViolationException(
                    "Authority already exists: " + dto.getName()
            );
        }

        authority.setName(dto.getName());
        return authorityMapper.toDto(authority);
    }

    // ✅ Delete

    public void deleteAuthority(Long id) {

        Authority authority = authorityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Authority not found with id: " + id)
                );

        boolean authorityInUse = !authority.getRoles().isEmpty();
        if (authorityInUse) {
            throw new EntityInUseException("Cannot delete authority because it is assigned to one or more roles.");
        }

        authorityRepository.delete(authority);
    }
}

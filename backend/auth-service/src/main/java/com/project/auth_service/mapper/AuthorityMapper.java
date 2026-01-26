package com.project.auth_service.mapper;

import com.project.auth_service.dto.*;
import com.project.auth_service.model.Authority;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthorityMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    Authority toEntity(AddAuthorityDto dto);

    AuthorityDto toDto(Authority authority);
}

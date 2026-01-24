package com.project.auth_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.project.auth_service.model.User;
import com.project.auth_service.dto.ProfileDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // تحويل كائن User إلى كائن ProfileDto
    @Mapping(target = "role", source = "role.name")
    @Mapping(
        target = "authorities",
        expression = "java(user.getRole().getAuthorities().stream().map(a -> a.getName()).collect(java.util.stream.Collectors.toList()))"
    )
    ProfileDto toProfileDto(User user);
}

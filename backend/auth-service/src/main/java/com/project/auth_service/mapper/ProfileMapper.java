package com.project.auth_service.mapper;

import com.project.auth_service.dto.ProfileResponse;
import com.project.auth_service.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.role.name", target = "role")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "martialStatus", target = "maritalStatus")
    ProfileResponse toProfile(Employee employee);
}

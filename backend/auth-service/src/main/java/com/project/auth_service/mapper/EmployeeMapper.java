package com.project.auth_service.mapper;

import com.project.auth_service.dto.EmployeeListResponse;
import com.project.auth_service.dto.EmployeeResponse;
import com.project.auth_service.model.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    // ===================== Single Employee =====================

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.role.name", target = "role")
    @Mapping(source = "user.enabled", target = "enabled")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "martialStatus", target = "maritalStatus")
    EmployeeResponse toResponse(Employee employee);

    // ===================== List =====================

    @Mapping(expression = "java(employee.getFirstName() + \" \" + employee.getLastName())", target = "fullName")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.role.name", target = "role")
    @Mapping(source = "user.enabled", target = "enabled")
    EmployeeListResponse toListResponse(Employee employee);
}

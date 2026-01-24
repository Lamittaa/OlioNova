package com.project.auth_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.project.auth_service.model.Role;
import com.project.auth_service.dto.*;

@Mapper(
    componentModel = "spring",
    imports = { java.util.stream.Collectors.class }   // ✅ يضيف import داخل الكود المولّد
)
public interface RoleMapper{

    // 🟢 خاص بـ POST /api/admin/roles (إنشاء دور جديد)
    Role toEntity(AddRoleDto dto);
    AddRoleResponseDto toAddRoleResponse(Role role);

    // 🟢 خاص بـ GET /api/admin/roles (عرض الأدوار)
    @Mapping(
        target = "authorities",
        expression = "java(role.getAuthorities().stream().map(a -> a.getName()).collect(Collectors.toList()))"
    )
    RoleDto toRoleDto(Role role);
}

package com.project.auth_service.controller;

import com.project.auth_service.dto.*;
import com.project.auth_service.mapper.EmployeeMapper;
import com.project.auth_service.mapper.ProfileMapper;
import com.project.auth_service.model.Employee;
import com.project.auth_service.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;
    private final ProfileMapper profileMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest req) {
        Employee saved = employeeService.createEmployee(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeMapper.toResponse(saved));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeeListResponse> getAll() {
        return employeeService.getAllEmployees()
                .stream()
                .map(employeeMapper::toListResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeMapper.toResponse(
                employeeService.getEmployeeById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest req) {
        Employee updated = employeeService.updateEmployee(id, req);
        return employeeMapper.toResponse(updated);
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    public ProfileResponse updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {

        Employee updated = employeeService.updateMyProfile(
                auth.getName(), req);

        return profileMapper.toProfile(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id) {
        employeeService.disableEmployee(id);
    }

    @GetMapping("/by-national-id/{nid}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse getByNationalId(@PathVariable String nid) {
        Employee employee = employeeService.getByNationalId(nid);
        return employeeMapper.toResponse(employee);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    public ProfileResponse myProfile(Authentication auth) {
        Employee emp = employeeService.getMyProfile(auth.getName());
        return profileMapper.toProfile(emp);
    }

    @PostMapping("/profile/change-password")
    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest req) {
        employeeService.changePassword(
                auth.getName(),
                req.getOldPassword(),
                req.getNewPassword());
    }

    @GetMapping("/internal/{id}")
public EmployeeResponse getEmployeeInternal(@PathVariable Long id) {
    return employeeMapper.toResponse(
            employeeService.getEmployeeById(id));
}
}

package com.project.auth_service.service;

import com.project.auth_service.dto.CreateEmployeeRequest;
import com.project.auth_service.dto.UpdateProfileRequest;
import com.project.auth_service.exception.*;
import com.project.auth_service.model.*;
import com.project.auth_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final EmailService emailService;
    private final PasswordEncoder encoder;

    // =========================================================
    // CREATE EMPLOYEE (ADMIN)
    // =========================================================
public Employee createEmployee(CreateEmployeeRequest req) {

    if (employeeRepo.existsByNationalId(req.getNationalId())) {
        throw new EntityAlreadyExistsException("National ID already exists");
    }

    if (employeeRepo.existsByEmail(req.getEmail())) {
        throw new EntityAlreadyExistsException("Email already exists");
    }

    Role role = roleRepo.findByName(req.getRoleName())
            .orElseThrow(() ->
                    new RoleNotFoundException("Role not found: " + req.getRoleName())
            );

    String username = generateUniqueUsername();

    String rawPassword = generateRandomPassword();

    // ---------- User ----------
    User user = User.builder()
            .username(username)
            .password(encoder.encode(rawPassword))
            .enabled(true)
            .role(role)
            .build();

    userRepo.save(user);

    // ---------- Employee ----------
    Employee employee = Employee.builder()
            .nationalId(req.getNationalId())
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .phoneNumber(req.getPhoneNumber())
            .email(req.getEmail())
            .gender(Gender.valueOf(req.getGender()))
            .martialStatus(MaritalStatus.valueOf(req.getMaritalStatus()))
            .user(user)
            .build();

    employeeRepo.save(employee);

    // ---------- Send Email ----------
    emailService.sendAccountEmail(
            employee.getEmail(),
            username,
            rawPassword
    );

    return employee;
}
    // =========================================================
    // GET ALL (ADMIN)
    // =========================================================
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepo.findAll();
    }

    // =========================================================
    // GET BY ID (ADMIN)
    // =========================================================
    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException("Employee not found with id=" + id)
                );
    }

    // =========================================================
    // UPDATE EMPLOYEE (ADMIN)
    // =========================================================


    // =========================================================
    // SOFT DELETE (ADMIN)
    // =========================================================
    public void disableEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employee.getUser().setEnabled(false);
    }

    // =========================================================
    // PROFILE (SELF)
    // =========================================================
    @Transactional(readOnly = true)
    public Employee getMyProfile(String username) {
        return employeeRepo.findByUserUsername(username)
                .orElseThrow(() ->
                        new EmployeeNotFoundException("Employee profile not found for user=" + username)
                );
    }

  @Transactional
public Employee updateMyProfile(String username, UpdateProfileRequest req) {

    Employee employee = getMyProfile(username);

    if (req.getFirstName() != null) {
        employee.setFirstName(req.getFirstName());
    }

    if (req.getLastName() != null) {
        employee.setLastName(req.getLastName());
    }

    if (req.getPhoneNumber() != null) {
        employee.setPhoneNumber(req.getPhoneNumber());
    }

    if (req.getEmail() != null) {
        employee.setEmail(req.getEmail());
    }

    if (req.getMaritalStatus() != null) {
        employee.setMartialStatus(
                MaritalStatus.valueOf(req.getMaritalStatus())
        );
    }

    return employee;
}


    // =========================================================
    // CHANGE PASSWORD (AFTER LOGIN)
    // =========================================================
    public void changePassword(String username, String oldPassword, String newPassword) {

        User user = userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found: " + username)
                );

        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPassword(encoder.encode(newPassword));
    }

    // =========================================================
    // USERNAME GENERATOR (UNIQUE)
    // =========================================================
    private String generateUniqueUsername() {
        String username;
        do {
            username = "EMP-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 6)
                    .toUpperCase();
        } while (userRepo.existsByUsername(username));
        return username;
    }


    


@Transactional(readOnly = true)
public Employee getByNationalId(String nationalId) {
    return employeeRepo.findByNationalId(nationalId)
            .orElseThrow(() ->
                    new EmployeeNotFoundException(
                            "Employee not found with nationalId=" + nationalId
                    )
            );
}

private String generateRandomPassword() {

    String chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789" +
            "!@#$%&*";

    StringBuilder password = new StringBuilder();

    java.util.Random random = new java.util.Random();

    for (int i = 0; i < 10; i++) {
        password.append(chars.charAt(random.nextInt(chars.length())));
    }

    return password.toString();
}
}
